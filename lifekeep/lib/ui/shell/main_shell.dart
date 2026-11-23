import 'package:flutter/material.dart';

import '../../core/l10n/lang.dart';
import '../calendar/calendar_screen.dart';
import '../design/design.dart';
import '../documents/documents_screen.dart';
import '../home/home_screen.dart';
import '../tasks/tasks_screen.dart';

class MainShell extends StatefulWidget {
  const MainShell({super.key});

  static void goToTab(BuildContext context, int index) =>
      context.findAncestorStateOfType<_MainShellState>()?._select(index);

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _index = 0;

  void _select(int i) => setState(() => _index = i);

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: _index == 0,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) setState(() => _index = 0);
      },
      child: Scaffold(
        body: LkBackdrop(
          child: IndexedStack(
            index: _index,
            children: const [
              HomeScreen(),
              DocumentsScreen(),
              TasksScreen(),
              CalendarScreen(),
            ],
          ),
        ),
        bottomNavigationBar: FloatingNavBar(
          selected: _index,
          onSelect: _select,
          items: [
            (
              Icons.home_outlined,
              Icons.home_rounded,
              context.tr('หน้าแรก', 'Home'),
            ),
            (
              Icons.folder_outlined,
              Icons.folder_rounded,
              context.tr('เอกสาร', 'Documents'),
            ),
            (
              Icons.check_circle_outline_rounded,
              Icons.check_circle_rounded,
              context.tr('งาน', 'Tasks'),
            ),
            (
              Icons.calendar_month_outlined,
              Icons.calendar_month_rounded,
              context.tr('ปฏิทิน', 'Calendar'),
            ),
          ],
        ),
      ),
    );
  }
}
