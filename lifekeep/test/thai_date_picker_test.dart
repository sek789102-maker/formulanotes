import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifekeep/core/theme/app_theme.dart';
import 'package:lifekeep/ui/widgets/thai_date_picker.dart';

void main() {
  testWidgets('shows Buddhist Era years and returns the picked date', (tester) async {
    DateTime? result;
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Builder(
          builder: (context) => TextButton(
            onPressed: () async => result = await showThaiDatePicker(
              context,
              initial: DateTime(2026, 11, 23),
              first: DateTime(2000),
              last: DateTime(2060),
            ),
            child: const Text('open'),
          ),
        ),
      ),
    );
    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();

    // Thai month with BE year (2026 → 2569), never the Gregorian year.
    expect(find.text('พฤศจิกายน 2569'), findsOneWidget);
    expect(find.textContaining('2026'), findsNothing);

    // Year grid lists BE years; pick 2570 (= 2027).
    await tester.tap(find.byKey(const Key('thai_picker_month')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('2570'));
    await tester.pumpAndSettle();
    expect(find.text('พฤศจิกายน 2570'), findsOneWidget);

    // Pick the 5th and confirm.
    await tester.tap(find.text('5').first);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('thai_picker_ok')));
    await tester.pumpAndSettle();
    expect(result, DateTime(2027, 11, 5));
  });

  testWidgets('cancel returns null', (tester) async {
    DateTime? result = DateTime(1999);
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Builder(
          builder: (context) => TextButton(
            onPressed: () async => result = await showThaiDatePicker(
              context,
              initial: DateTime(2026, 1, 31),
              first: DateTime(2000),
              last: DateTime(2060),
            ),
            child: const Text('open'),
          ),
        ),
      ),
    );
    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('thai_picker_cancel')));
    await tester.pumpAndSettle();
    expect(result, isNull);
  });
}
