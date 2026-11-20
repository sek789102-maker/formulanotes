import 'package:flutter/material.dart';

import '../../core/l10n/lang.dart';
import '../calendar/calendar_screen.dart';
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
        body: IndexedStack(
          index: _index,
          children: const [HomeScreen(), DocumentsScreen(), TasksScreen(), CalendarScreen()],
        ),
        bottomNavigationBar: NavigationBar(
          selectedIndex: _index,
          onDestinationSelected: _select,
          destinations: [
            NavigationDestination(
              icon: const Icon(Icons.home_outlined),
              selectedIcon: const Icon(Icons.home_rounded),
              label: context.tr('หน้าแรก', 'Home'),
            ),
            NavigationDestination(
              icon: const Icon(Icons.folder_outlined),
              selectedIcon: const Icon(Icons.folder_rounded),
              label: context.tr('เอกสาร', 'Documents'),
            ),
            NavigationDestination(
              icon: const Icon(Icons.check_circle_outline_rounded),
              selectedIcon: const Icon(Icons.check_circle_rounded),
              label: context.tr('งาน', 'Tasks'),
            ),
            NavigationDestination(
              icon: const Icon(Icons.calendar_month_outlined),
              selectedIcon: const Icon(Icons.calendar_month_rounded),
              label: context.tr('ปฏิทิน', 'Calendar'),
            ),
          ],
        ),
      ),
    );
  }
}
