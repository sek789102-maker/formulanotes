import 'package:flutter_test/flutter_test.dart';
import 'package:lifekeep/core/l10n/lang.dart';

import 'helpers.dart';

void main() {
  test('default categories follow the app language; user-renamed ones are kept', () async {
    final env = await TestEnv.create();
    addTearDown(env.dispose);
    final c = env.controller;
    expect(c.categoryById('cat_vehicle').name, 'ยานพาหนะ');
    // User renames one category
    await c.saveCategory(c.categoryById('cat_home').copyWith(name: 'คอนโด'));

    await c.setLanguage(AppLanguage.en);
    expect(c.categoryById('cat_vehicle').name, 'Vehicle');
    expect(c.categoryById('cat_other').name, 'Other');
    expect(c.categoryById('cat_home').name, 'คอนโด', reason: 'user-renamed category must not change');

    await c.setLanguage(AppLanguage.th);
    expect(c.categoryById('cat_vehicle').name, 'ยานพาหนะ');
    expect(c.categoryById('cat_home').name, 'คอนโด');
  });
}
