import 'dart:async';

import 'package:flutter/material.dart';
import 'package:softpos_sdk/softpos_sdk.dart';

void main() => runApp(const MaterialApp(home: DemoPage()));

class DemoPage extends StatefulWidget {
  const DemoPage({super.key});

  @override
  State<DemoPage> createState() => _DemoPageState();
}

class _DemoPageState extends State<DemoPage> {
  // The two MineSec SoftPOS apps this demo can drive. Swap to your own.
  static const _prod = 'com.minesec.tabadul';
  static const _stage = 'com.minesec.tabadul.stage';

  final _amount = TextEditingController(text: '10.00');
  final _svc = SoftPosService();
  StreamSubscription<SoftPosResult>? _sub;

  String _msaPackage = _prod;
  bool _busy = false;
  String? _result;

  @override
  void initState() {
    super.initState();
    _reinit();
  }

  Future<void> _reinit() async {
    await _svc.dispose();
    await _sub?.cancel();
    await _svc.init(msaPackage: _msaPackage);
    _sub = _svc.stream.listen((r) {
      setState(() {
        _busy = false;
        _result = r.toString();
      });
    });
  }

  @override
  void dispose() {
    _sub?.cancel();
    _svc.dispose();
    _amount.dispose();
    super.dispose();
  }

  Future<void> _sale() async {
    if (!await _svc.isSoftPosInstalled()) {
      setState(() => _result = 'MineSec SoftPOS ($_msaPackage) not installed');
      return;
    }
    setState(() {
      _busy = true;
      _result = null;
    });
    await _svc.startSaleTransaction(
      amount: double.tryParse(_amount.text.trim()) ?? 0,
      posMessageId: DateTime.now().millisecondsSinceEpoch.toString(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('softpos_sdk demo')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: _prod, label: Text('prod')),
                ButtonSegment(value: _stage, label: Text('stage')),
              ],
              selected: {_msaPackage},
              onSelectionChanged: (s) {
                setState(() => _msaPackage = s.first);
                _reinit();
              },
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _amount,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              decoration: const InputDecoration(
                labelText: 'Amount',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _busy ? null : _sale,
              child: _busy ? const Text('Processing…') : const Text('Sale'),
            ),
            if (_result != null) ...[
              const SizedBox(height: 24),
              SelectableText(
                _result!,
                style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
