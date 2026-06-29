package com.elitesoft.softpos_sdk

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.theminesec.app.poslib.MsaPosApi
import com.theminesec.app.poslib.model.PosRequest
import com.theminesec.app.poslib.model.PosResponse
import com.theminesec.app.poslib.model.TransactionResponse
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.math.BigDecimal

/**
 * Bridges the MineSec SoftPOS (poslib) sale / warmUp / activation flows to Flutter.
 *
 * The host MUST use [io.flutter.embedding.android.FlutterFragmentActivity] (a
 * ComponentActivity) — the MineSec activity-result contracts need one. The MSA
 * package name (prod vs stage) is passed from Dart via the `init` method, so
 * contracts are registered lazily through [ComponentActivity.getActivityResultRegistry],
 * which (unlike the lifecycle-bound helper) may be called at any time.
 */
class MinesecSoftposPlugin : FlutterPlugin, ActivityAware, MethodChannel.MethodCallHandler {

    private companion object {
        const val METHOD_CHANNEL = "com.elitesoft.softpos_sdk/channel"
        const val EVENT_CHANNEL = "com.elitesoft.softpos_sdk/events"
        const val KEY_TXN = "softpos_sdk_txn"
        const val KEY_WARMUP = "softpos_sdk_warmup"
        const val KEY_ACTIVATION = "softpos_sdk_activation"
    }

    private var methodChannel: MethodChannel? = null
    private var eventChannel: EventChannel? = null
    private var eventSink: EventChannel.EventSink? = null

    private var activity: ComponentActivity? = null
    private var msaPosApi: MsaPosApi? = null
    private var registeredPackage: String? = null

    private var transactionLauncher: ActivityResultLauncher<PosRequest.Transaction>? = null
    private var warmUpLauncher: ActivityResultLauncher<Unit>? = null
    private var activationLauncher: ActivityResultLauncher<PosRequest.Activation>? = null

    // FlutterPlugin

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel = MethodChannel(binding.binaryMessenger, METHOD_CHANNEL).apply {
            setMethodCallHandler(this@MinesecSoftposPlugin)
        }
        eventChannel = EventChannel(binding.binaryMessenger, EVENT_CHANNEL).apply {
            setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    eventSink = events
                }
                override fun onCancel(arguments: Any?) {
                    eventSink = null
                }
            })
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel?.setMethodCallHandler(null)
        eventChannel?.setStreamHandler(null)
        methodChannel = null
        eventChannel = null
    }

    // ActivityAware

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity as? ComponentActivity
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity as? ComponentActivity
        // Re-register against the new activity instance if init() already ran.
        registeredPackage?.let { registerLaunchers(it) }
    }

    override fun onDetachedFromActivityForConfigChanges() {
        unregisterLaunchers()
        activity = null
    }

    override fun onDetachedFromActivity() {
        unregisterLaunchers()
        activity = null
    }

    // MethodCallHandler

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "init" -> {
                val pkg = call.argument<String>("msaPackage")
                if (pkg.isNullOrEmpty()) {
                    result.error("BAD_ARGS", "msaPackage is required", null)
                    return
                }
                if (activity == null) {
                    result.error("NO_ACTIVITY", "Host activity not attached (use FlutterFragmentActivity)", null)
                    return
                }
                registerLaunchers(pkg)
                result.success(null)
            }
            "isSoftPosInstalled" -> result.success(isSoftPosInstalled())
            "startSaleTransaction" -> {
                val amount = call.argument<String>("amount") ?: "0"
                val posMessageId = call.argument<String>("posMessageId") ?: ""
                val autoDismiss = call.argument<Boolean>("autoDismissResult") ?: true
                launchSaleTransaction(amount, posMessageId, autoDismiss, result)
            }
            "warmUp" -> launchWarmUp(result)
            "activate" -> launchActivation(call.argument<String>("activationCode") ?: "", result)
            else -> result.notImplemented()
        }
    }

    // SoftPOS wiring

    private fun registerLaunchers(pkg: String) {
        val act = activity ?: return
        if (registeredPackage == pkg && transactionLauncher != null) return
        unregisterLaunchers()

        val api = MsaPosApi(pkg)
        msaPosApi = api
        val registry = act.activityResultRegistry

        transactionLauncher = registry.register(KEY_TXN, api.transactionContract()) { response ->
            when (response) {
                is PosResponse.Success<*> -> {
                    val tx = response.data as? TransactionResponse
                    if (tx != null) {
                        eventSink?.success(mapOf(
                            "isSuccess" to true,
                            "data" to mapOf(
                                "tranId" to tx.tranId,
                                "trace" to tx.trace,
                                "rrn" to tx.rrn,
                                "tranType" to tx.tranType,
                                "tranStatus" to tx.tranStatus,
                                "approvalCode" to tx.approvalCode,
                                "paymentMethod" to tx.paymentMethod,
                                "entryMode" to tx.entryMode,
                                "maskedAccount" to tx.maskedAccount,
                                "cvmPerformed" to tx.cvmPerformed,
                                "acqMid" to tx.acqMid,
                                "acqTid" to tx.acqTid,
                                "posMessageId" to tx.posMessageId,
                                "mchAddress" to tx.mchAddress,
                                "mchName" to tx.mchName,
                                "totalAmount" to tx.totalAmount.toDouble(),
                                "createByName" to tx.createByName,
                                "createdAt" to tx.createdAt,
                                "updatedAt" to tx.updatedAt,
                            )
                        ))
                    } else {
                        eventSink?.success(mapOf("isSuccess" to false, "errorMessage" to "Transaction data is null"))
                    }
                }
                is PosResponse.Failed -> eventSink?.success(mapOf(
                    "isSuccess" to false,
                    "errorCode" to response.rspCode,
                    "errorMessage" to response.rspMsg,
                ))
                else -> eventSink?.success(mapOf("isSuccess" to false, "errorMessage" to "Unknown response"))
            }
        }

        warmUpLauncher = registry.register(KEY_WARMUP, api.warmUpContract()) { response ->
            emitSimple(response)
        }

        activationLauncher = registry.register(KEY_ACTIVATION, api.activationContract()) { response ->
            emitSimple(response)
        }

        registeredPackage = pkg
    }

    private fun emitSimple(response: PosResponse<*>) {
        when (response) {
            is PosResponse.Success<*> -> eventSink?.success(mapOf("isSuccess" to true))
            is PosResponse.Failed -> eventSink?.success(mapOf(
                "isSuccess" to false,
                "errorCode" to response.rspCode,
                "errorMessage" to response.rspMsg,
            ))
            else -> eventSink?.success(mapOf("isSuccess" to false, "errorMessage" to "Unknown response"))
        }
    }

    private fun unregisterLaunchers() {
        transactionLauncher?.unregister()
        warmUpLauncher?.unregister()
        activationLauncher?.unregister()
        transactionLauncher = null
        warmUpLauncher = null
        activationLauncher = null
        registeredPackage = null
    }

    private fun isSoftPosInstalled(): Boolean {
        val pkg = registeredPackage ?: return false
        val pm = activity?.packageManager ?: return false
        return try {
            pm.getApplicationInfo(pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            try {
                pm.getPackageInfo(pkg, PackageManager.MATCH_DEFAULT_ONLY)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                pm.getLaunchIntentForPackage(pkg) != null
            }
        } catch (_: Exception) {
            msaPosApi?.isSoftPosInstalled(activity!!) ?: false
        }
    }

    private fun launchSaleTransaction(
        amountStr: String,
        posMessageId: String,
        autoDismissResult: Boolean,
        result: MethodChannel.Result,
    ) {
        val launcher = transactionLauncher
            ?: return result.error("NOT_INITIALIZED", "SoftPOS not initialized — call init()", null)
        // poslib assumes a 2-decimal currency: it sends minor units as
        // amount.movePointRight(2).toLong(). IQD has 3 decimals, so the POS app reads
        // that long as fils (/1000). Multiply by 10 to bridge the 2->3 decimal gap:
        // 10.0 IQD -> 100 -> poslib makes 10000 fils -> POS shows 10.000.
        val amount = amountStr.toBigDecimal().movePointRight(1)
        android.util.Log.d("MinesecSoftpos", "startSaleTransaction amount=$amount (scale=${amount.scale()}, unscaled=${amount.unscaledValue()})")
        result.success(null)
        launcher.launch(
            PosRequest.Transaction.Sale(
                amount = amount,
                posMessageId = posMessageId,
                autoDismissResult = autoDismissResult,
            )
        )
    }

    private fun launchWarmUp(result: MethodChannel.Result) {
        val launcher = warmUpLauncher
            ?: return result.error("NOT_INITIALIZED", "SoftPOS not initialized — call init()", null)
        result.success(null)
        launcher.launch(Unit)
    }

    private fun launchActivation(code: String, result: MethodChannel.Result) {
        val launcher = activationLauncher
            ?: return result.error("NOT_INITIALIZED", "SoftPOS not initialized — call init()", null)
        result.success(null)
        launcher.launch(PosRequest.Activation(activationCode = code))
    }
}
