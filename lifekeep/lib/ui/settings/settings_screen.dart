import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/app_info.dart';
import '../../core/l10n/lang.dart';
import '../../core/theme/app_theme.dart';
import '../../state/app_controller.dart';
import '../widgets/common.dart';
import 'about_screens.dart';
import 'app_lock_screen.dart';
import 'backup_screen.dart';
import 'categories_screen.dart';
import 'delete_all_flow.dart';
import 'notification_settings_screen.dart';
import 'storage_screen.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    void open(Widget page) => Navigator.push(context, MaterialPageRoute(builder: (_) => page));
    final themeLabel = switch (c.themeMode) {
      ThemeMode.light => context.tr('สว่าง', 'Light'),
      ThemeMode.dark => context.tr('มืด', 'Dark'),
      ThemeMode.system => context.tr('ตามระบบ', 'System'),
    };
    return Scaffold(
      appBar: AppBar(title: Text(context.tr('ตั้งค่า', 'Settings'))),
      body: ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 32), children: [
        _Group(title: context.tr('การแสดงผล', 'Appearance'), children: [
          _Tile(
            icon: Icons.palette_outlined,
            title: context.tr('ธีม', 'Theme'),
            value: themeLabel,
            onTap: () => _chooseTheme(context, c),
          ),
          _Tile(
            icon: Icons.translate_rounded,
            title: context.tr('ภาษา', 'Language'),
            value: c.language == AppLanguage.en ? 'English' : 'ไทย',
            onTap: () => _chooseLanguage(context, c),
          ),
        ]),
        _Group(title: context.tr('การแจ้งเตือนและความปลอดภัย', 'Reminders & security'), children: [
          _Tile(
            icon: Icons.notifications_none_rounded,
            title: context.tr('การแจ้งเตือน', 'Notifications'),
            value: c.reminderPrefs.enabled ? context.tr('เปิด', 'On') : context.tr('ปิด', 'Off'),
            onTap: () => open(const NotificationSettingsScreen()),
          ),
          _Tile(
            icon: Icons.lock_outline_rounded,
            title: context.tr('ล็อกแอป', 'App lock'),
            value: c.lockEnabled ? context.tr('เปิด', 'On') : context.tr('ปิด', 'Off'),
            onTap: () => open(const AppLockSettingsScreen()),
          ),
        ]),
        _Group(title: context.tr('ข้อมูล', 'Data'), children: [
          _Tile(
            icon: Icons.backup_outlined,
            title: context.tr('สำรองและกู้คืนข้อมูล', 'Backup & restore'),
            onTap: () => open(const BackupScreen()),
          ),
          _Tile(
            icon: Icons.label_outline_rounded,
            title: context.tr('จัดการหมวดหมู่', 'Manage categories'),
            value: '${c.categories.length}',
            onTap: () => open(const CategoriesScreen()),
          ),
          _Tile(
            icon: Icons.storage_rounded,
            title: context.tr('พื้นที่จัดเก็บ', 'Storage'),
            onTap: () => open(const StorageScreen()),
          ),
        ]),
        _Group(title: context.tr('เกี่ยวกับ', 'About'), children: [
          _Tile(
            icon: Icons.info_outline_rounded,
            title: context.tr('เกี่ยวกับ LIFEKEEP', 'About LIFEKEEP'),
            onTap: () => open(const AboutScreen()),
          ),
          _Tile(
            icon: Icons.privacy_tip_outlined,
            title: context.tr('ความเป็นส่วนตัว', 'Privacy'),
            onTap: () => open(const PrivacyScreen()),
          ),
          _Tile(
            icon: Icons.tag_rounded,
            title: context.tr('เวอร์ชัน', 'Version'),
            value: '${AppInfo.version} (${AppInfo.build})',
          ),
        ]),
        _Group(title: context.tr('โซนอันตราย', 'Danger zone'), children: [
          ListTile(
            leading: Icon(Icons.delete_forever_outlined, color: context.status.danger),
            title: Text(context.tr('ลบข้อมูลทั้งหมด', 'Delete all data'), style: TextStyle(color: context.status.danger)),
            subtitle: Text(context.tr('ต้องยืนยัน 2 ขั้นตอน', 'Requires 2-step confirmation')),
            onTap: () => startDeleteAllFlow(context),
          ),
        ]),
      ]),
    );
  }

  Future<void> _chooseTheme(BuildContext context, AppController c) async {
    final r = await showModalBottomSheet<ThemeMode>(
      context: context,
      builder: (ctx) => SafeArea(
        child: RadioGroup<ThemeMode>(
          groupValue: c.themeMode,
          onChanged: (v) => Navigator.pop(ctx, v),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            for (final m in ThemeMode.values)
              RadioListTile<ThemeMode>(
                value: m,
                title: Text(switch (m) {
                  ThemeMode.system => ctx.tr('ตามระบบ', 'System'),
                  ThemeMode.light => ctx.tr('สว่าง', 'Light'),
                  ThemeMode.dark => ctx.tr('มืด', 'Dark'),
                }),
              ),
            const SizedBox(height: 8),
          ]),
        ),
      ),
    );
    if (r != null && context.mounted) await runAction(context, () => c.setThemeMode(r));
  }

  Future<void> _chooseLanguage(BuildContext context, AppController c) async {
    final r = await showModalBottomSheet<AppLanguage>(
      context: context,
      builder: (ctx) => SafeArea(
        child: RadioGroup<AppLanguage>(
          groupValue: c.language,
          onChanged: (v) => Navigator.pop(ctx, v),
          child: const Column(mainAxisSize: MainAxisSize.min, children: [
            RadioListTile<AppLanguage>(value: AppLanguage.th, title: Text('ไทย')),
            RadioListTile<AppLanguage>(value: AppLanguage.en, title: Text('English')),
            SizedBox(height: 8),
          ]),
        ),
      ),
    );
    if (r != null && context.mounted) await runAction(context, () => c.setLanguage(r));
  }
}

class _Group extends StatelessWidget {
  const _Group({required this.title, required this.children});
  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(8, 20, 8, 8),
        child: Text(title,
            style: Theme.of(context).textTheme.labelLarge?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant)),
      ),
      Card(
        child: Column(children: [
          for (var i = 0; i < children.length; i++) ...[
            if (i > 0) const Divider(indent: 56),
            children[i],
          ],
        ]),
      ),
    ]);
  }
}

class _Tile extends StatelessWidget {
  const _Tile({required this.icon, required this.title, this.value, this.onTap});
  final IconData icon;
  final String title;
  final String? value;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return ListTile(
      leading: Icon(icon),
      title: Text(title),
      onTap: onTap,
      trailing: Row(mainAxisSize: MainAxisSize.min, children: [
        if (value != null) Text(value!, style: TextStyle(color: scheme.onSurfaceVariant)),
        if (onTap != null) Icon(Icons.chevron_right_rounded, color: scheme.onSurfaceVariant),
      ]),
    );
  }
}
