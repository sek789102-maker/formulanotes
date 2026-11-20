import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:provider/provider.dart';

import 'core/app_info.dart';
import 'core/l10n/lang.dart';
import 'core/theme/app_theme.dart';
import 'core/utils/errors.dart';
import 'data/database/app_database.dart';
import 'services/attachment_service.dart';
import 'services/notification_gateway.dart';
import 'services/security_service.dart';
import 'state/app_controller.dart';
import 'ui/lock/lock_gate.dart';
import 'ui/shell/main_shell.dart';

/// Opens the database and services, showing a friendly error screen if
/// something goes wrong (never a crash or stack trace).
class BootstrapApp extends StatefulWidget {
  const BootstrapApp({super.key});

  @override
  State<BootstrapApp> createState() => _BootstrapAppState();
}

class _BootstrapAppState extends State<BootstrapApp> {
  late Future<AppController> _future = _init();

  Future<AppController> _init() async {
    final systemEn = ui.PlatformDispatcher.instance.locale.languageCode == 'en';
    final support = await getApplicationSupportDirectory();
    final db = await AppDatabase.open(englishDefaults: systemEn);
    final services = AppServices(
      db: db,
      gateway: LocalNotificationGateway(),
      security: SecurityService(FlutterSecureKV()),
      attachments: AttachmentService(support),
      backupDir: Directory(p.join(support.path, 'backups')),
      appVersion: AppInfo.version,
    );
    final c = AppController(services);
    await c.load();
    return c;
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<AppController>(
      future: _future,
      builder: (context, snap) {
        if (snap.hasData) return LifeKeepApp(controller: snap.data!);
        return MaterialApp(
          debugShowCheckedModeBanner: false,
          theme: AppTheme.light(),
          darkTheme: AppTheme.dark(),
          home: Scaffold(
            body: Center(
              child: snap.hasError
                  ? Padding(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.error_outline_rounded, size: 48, color: AppColors.danger),
                          const SizedBox(height: 16),
                          Text(friendlyError(snap.error!), textAlign: TextAlign.center),
                          const SizedBox(height: 16),
                          FilledButton(
                            onPressed: () => setState(() => _future = _init()),
                            child: Text(Lang.t('ลองใหม่', 'Retry')),
                          ),
                        ],
                      ),
                    )
                  : const CircularProgressIndicator(),
            ),
          ),
        );
      },
    );
  }
}

class LifeKeepApp extends StatefulWidget {
  const LifeKeepApp({super.key, required this.controller});
  final AppController controller;

  @override
  State<LifeKeepApp> createState() => _LifeKeepAppState();
}

class _LifeKeepAppState extends State<LifeKeepApp> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => widget.controller.startupTasks());
  }

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider.value(
      value: widget.controller,
      child: Consumer<AppController>(
        builder: (context, c, _) => MaterialApp(
          title: AppInfo.name,
          debugShowCheckedModeBanner: false,
          theme: AppTheme.light(),
          darkTheme: AppTheme.dark(),
          themeMode: c.themeMode,
          locale: c.locale,
          supportedLocales: const [Locale('th'), Locale('en')],
          localizationsDelegates: const [
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
          // The lock gate wraps the Navigator so pushed pages are covered too.
          builder: (context, child) => AppLockGate(child: child ?? const SizedBox()),
          home: const MainShell(),
        ),
      ),
    );
  }
}
