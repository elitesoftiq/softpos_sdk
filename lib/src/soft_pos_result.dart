class SoftPosTransactionData {
  final String tranId;
  final String trace;
  final String rrn;
  final String tranType;
  final String tranStatus;
  final String approvalCode;
  final String paymentMethod;
  final String entryMode;
  final String maskedAccount;
  final String cvmPerformed;
  final String acqMid;
  final String acqTid;
  final String posMessageId;
  final String mchAddress;
  final String mchName;
  final double totalAmount;
  final String createByName;
  final String createdAt;
  final String updatedAt;

  const SoftPosTransactionData({
    required this.tranId,
    required this.trace,
    required this.rrn,
    required this.tranType,
    required this.tranStatus,
    required this.approvalCode,
    required this.paymentMethod,
    required this.entryMode,
    required this.maskedAccount,
    required this.cvmPerformed,
    required this.acqMid,
    required this.acqTid,
    required this.posMessageId,
    required this.mchAddress,
    required this.mchName,
    required this.totalAmount,
    required this.createByName,
    required this.createdAt,
    required this.updatedAt,
  });

  factory SoftPosTransactionData.fromMap(Map<dynamic, dynamic> map) {
    return SoftPosTransactionData(
      tranId: map['tranId'] as String? ?? '',
      trace: map['trace'] as String? ?? '',
      rrn: map['rrn'] as String? ?? '',
      tranType: map['tranType'] as String? ?? '',
      tranStatus: map['tranStatus'] as String? ?? '',
      approvalCode: map['approvalCode'] as String? ?? '',
      paymentMethod: map['paymentMethod'] as String? ?? '',
      entryMode: map['entryMode'] as String? ?? '',
      maskedAccount: map['maskedAccount'] as String? ?? '',
      cvmPerformed: map['cvmPerformed'] as String? ?? '',
      acqMid: map['acqMid'] as String? ?? '',
      acqTid: map['acqTid'] as String? ?? '',
      posMessageId: map['posMessageId'] as String? ?? '',
      mchAddress: map['mchAddress'] as String? ?? '',
      mchName: map['mchName'] as String? ?? '',
      totalAmount: (map['totalAmount'] as num?)?.toDouble() ?? 0.0,
      createByName: map['createByName'] as String? ?? '',
      createdAt: map['createdAt'] as String? ?? '',
      updatedAt: map['updatedAt'] as String? ?? '',
    );
  }

  @override
  String toString() => '''
tranId        : $tranId
trace         : $trace
rrn           : $rrn
tranType      : $tranType
tranStatus    : $tranStatus
approvalCode  : $approvalCode
paymentMethod : $paymentMethod
entryMode     : $entryMode
maskedAccount : $maskedAccount
cvmPerformed  : $cvmPerformed
acqMid        : $acqMid
acqTid        : $acqTid
posMessageId  : $posMessageId
mchAddress    : $mchAddress
mchName       : $mchName
totalAmount   : $totalAmount
createByName  : $createByName
createdAt     : $createdAt
updatedAt     : $updatedAt''';
}

sealed class SoftPosResult {
  const SoftPosResult();
}

final class SoftPosSuccess extends SoftPosResult {
  final SoftPosTransactionData data;
  const SoftPosSuccess(this.data);
}

final class SoftPosFailure extends SoftPosResult {
  final String? errorCode;
  final String? errorMessage;
  const SoftPosFailure({this.errorCode, this.errorMessage});

  @override
  String toString() => 'errorCode: $errorCode\nerrorMessage: $errorMessage';
}
