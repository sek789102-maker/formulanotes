import 'package:flutter/animation.dart';
import 'package:flutter/painting.dart';

/// LIFEKEEP design tokens. Every screen uses these instead of magic numbers
/// so spacing, corner radii and motion stay consistent across the app.
class Space {
  Space._();
  static const double xxs = 2;
  static const double xs = 4;
  static const double sm = 8;
  static const double md = 12;
  static const double lg = 16;
  static const double xl = 20;
  static const double xxl = 24;
  static const double xxxl = 32;

  /// Horizontal page padding.
  static const double page = 16;

  /// Content never grows wider than this (tablets / landscape).
  static const double maxContentWidth = 720;

  static const pagePadding = EdgeInsets.symmetric(horizontal: page);
}

class Radii {
  Radii._();
  static const double xs = 6;
  static const double sm = 10;
  static const double md = 14;
  static const double lg = 22;
  static const double xl = 28;
  static const double pill = 999;

  static final brSm = BorderRadius.circular(sm);
  static final brMd = BorderRadius.circular(md);
  static final brLg = BorderRadius.circular(lg);
}

class Motion {
  Motion._();
  static const fast = Duration(milliseconds: 150);
  static const medium = Duration(milliseconds: 240);
  static const slow = Duration(milliseconds: 360);
  static const curve = Curves.easeOutCubic;
  static const emphasized = Cubic(0.2, 0.0, 0, 1.0);
}

/// Component sizes.
class Sizes {
  Sizes._();
  static const double button = 48;
  static const double buttonCompact = 40;
  static const double row = 56;
  static const double iconBadge = 36;
  static const double thumb = 44;
}
