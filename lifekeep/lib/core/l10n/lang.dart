import 'package:flutter/widgets.dart';

/// Supported UI languages. Thai is the primary language; English is
/// available and every user-facing string is written as a (th, en) pair so
/// further languages can be added without restructuring screens.
enum AppLanguage { th, en }

class Lang {
  Lang._();

  /// Language used outside the widget tree (notifications, errors).
  static AppLanguage current = AppLanguage.th;

  static bool get isEn => current == AppLanguage.en;

  static String t(String th, String en) => isEn ? en : th;

  static AppLanguage parse(String? v) =>
      v == 'en' ? AppLanguage.en : AppLanguage.th;
}

extension TrX on BuildContext {
  /// Translate. Depends on [Localizations] so widgets rebuild on change.
  String tr(String th, String en) {
    final code = Localizations.maybeLocaleOf(this)?.languageCode;
    if (code == null) return Lang.t(th, en);
    return code == 'en' ? en : th;
  }
}
