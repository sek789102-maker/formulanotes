import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../state/app_controller.dart';
import 'lock_screen.dart';

/// Lets the app open system pickers (gallery, files, camera) without being
/// locked when it comes back.
class LockGuard {
  LockGuard._();
  static int _external = 0;
  static bool get suppressed => _external > 0;

  static Future<T> external<T>(Future<T> Function() action) async {
    _external++;
    try {
      return await action();
    } finally {
      // The "resumed" lifecycle event arrives slightly after the picker returns.
      Future.delayed(const Duration(milliseconds: 800), () => _external--);
    }
  }
}

class AppLockGate extends StatefulWidget {
  const AppLockGate({super.key, required this.child});
  final Widget child;

  @override
  State<AppLockGate> createState() => _AppLockGateState();
}

class _AppLockGateState extends State<AppLockGate> with WidgetsBindingObserver {
  bool _locked = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _locked = context.read<AppController>().lockEnabled;
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    final c = context.read<AppController>();
    if (state == AppLifecycleState.paused || state == AppLifecycleState.hidden) {
      if (c.lockEnabled && !LockGuard.suppressed && !_locked) setState(() => _locked = true);
    } else if (state == AppLifecycleState.resumed) {
      c.onResume();
    }
  }

  @override
  Widget build(BuildContext context) {
    final lockEnabled = context.select<AppController, bool>((c) => c.lockEnabled);
    final showLock = _locked && lockEnabled;
    return Stack(
      children: [
        // Keep the app alive (state preserved) but invisible while locked.
        Offstage(
          offstage: showLock,
          child: TickerMode(enabled: !showLock, child: widget.child),
        ),
        if (showLock) Positioned.fill(child: LockScreen(onUnlocked: () => setState(() => _locked = false))),
      ],
    );
  }
}
