import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/errors.dart';
import '../../state/app_controller.dart';

/// Runs [action], showing a friendly Thai/English message on failure and
/// an optional [success] message. Never shows stack traces.
Future<bool> runAction(
  BuildContext context,
  Future<void> Function() action, {
  String? success,
}) async {
  final messenger = ScaffoldMessenger.maybeOf(context);
  final errorColor = Theme.of(context).colorScheme.error;
  try {
    await action();
    if (success != null) {
      messenger?.hideCurrentSnackBar();
      messenger?.showSnackBar(SnackBar(content: Text(success)));
    }
    return true;
  } catch (e) {
    messenger?.hideCurrentSnackBar();
    messenger?.showSnackBar(SnackBar(
      content: Text(friendlyError(e)),
      backgroundColor: errorColor,
    ));
    return false;
  }
}

void showSnack(BuildContext context, String message, {bool error = false}) {
  final m = ScaffoldMessenger.maybeOf(context);
  m?.hideCurrentSnackBar();
  m?.showSnackBar(SnackBar(
    content: Text(message),
    backgroundColor: error ? Theme.of(context).colorScheme.error : null,
  ));
}

Future<bool> confirmDialog(
  BuildContext context, {
  required String title,
  required String message,
  required String confirmLabel,
  bool destructive = false,
  IconData? icon,
}) async {
  final scheme = Theme.of(context).colorScheme;
  final r = await showDialog<bool>(
    context: context,
    builder: (ctx) => AlertDialog(
      icon: icon == null ? null : Icon(icon, color: destructive ? scheme.error : scheme.primary),
      title: Text(title),
      content: Text(message),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text(ctx.tr('ยกเลิก', 'Cancel'))),
        FilledButton(
          style: destructive ? FilledButton.styleFrom(backgroundColor: scheme.error, foregroundColor: scheme.onError) : null,
          onPressed: () => Navigator.pop(ctx, true),
          child: Text(confirmLabel),
        ),
      ],
    ),
  );
  return r == true;
}

/// Loads data with [load] and re-runs it whenever the app data revision
/// changes, keeping the previous result visible while reloading (no flicker).
class DataBuilder<T> extends StatefulWidget {
  const DataBuilder({super.key, required this.load, required this.builder, this.deps = const []});

  final Future<T> Function(AppController c) load;
  final Widget Function(BuildContext context, T data) builder;

  /// Extra values (e.g. search query, filters) that trigger a reload.
  final List<Object?> deps;

  @override
  State<DataBuilder<T>> createState() => _DataBuilderState<T>();
}

class _DataBuilderState<T> extends State<DataBuilder<T>> {
  T? _data;
  Object? _error;
  int _rev = -1;
  List<Object?> _deps = const [];
  int _ticket = 0;

  bool _depsChanged() {
    if (_deps.length != widget.deps.length) return true;
    for (var i = 0; i < _deps.length; i++) {
      if (_deps[i] != widget.deps[i]) return true;
    }
    return false;
  }

  Future<void> _reload(AppController c) async {
    final ticket = ++_ticket;
    try {
      final d = await widget.load(c);
      if (!mounted || ticket != _ticket) return;
      setState(() {
        _data = d;
        _error = null;
      });
    } catch (e) {
      if (!mounted || ticket != _ticket) return;
      setState(() => _error = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    if (c.revision != _rev || _depsChanged()) {
      _rev = c.revision;
      _deps = List.of(widget.deps);
      WidgetsBinding.instance.addPostFrameCallback((_) => _reload(c));
    }
    if (_error != null && _data == null) {
      return ErrorView(message: friendlyError(_error!), onRetry: () => _reload(c));
    }
    final d = _data;
    if (d == null) return const Center(child: Padding(padding: EdgeInsets.all(32), child: CircularProgressIndicator()));
    return widget.builder(context, d);
  }
}

class ErrorView extends StatelessWidget {
  const ErrorView({super.key, required this.message, this.onRetry});
  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Icon(Icons.error_outline_rounded, size: 48, color: Theme.of(context).colorScheme.error),
          const SizedBox(height: 12),
          Text(message, textAlign: TextAlign.center),
          if (onRetry != null) ...[
            const SizedBox(height: 16),
            OutlinedButton(onPressed: onRetry, child: Text(context.tr('ลองใหม่', 'Retry'))),
          ],
        ]),
      ),
    );
  }
}

class EmptyState extends StatelessWidget {
  const EmptyState({
    super.key,
    required this.icon,
    required this.title,
    this.message,
    this.actionLabel,
    this.onAction,
    this.compact = false,
  });

  final IconData icon;
  final String title;
  final String? message;
  final String? actionLabel;
  final VoidCallback? onAction;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    return Center(
      child: Padding(
        padding: EdgeInsets.symmetric(horizontal: 32, vertical: compact ? 20 : 48),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: compact ? 56 : 76,
              height: compact ? 56 : 76,
              decoration: BoxDecoration(color: scheme.primaryContainer.withValues(alpha: 0.6), shape: BoxShape.circle),
              child: Icon(icon, size: compact ? 26 : 34, color: scheme.primary),
            ),
            SizedBox(height: compact ? 12 : 18),
            Text(title, style: text.titleMedium, textAlign: TextAlign.center),
            if (message != null) ...[
              const SizedBox(height: 6),
              Text(message!, style: text.bodyMedium?.copyWith(color: scheme.onSurfaceVariant), textAlign: TextAlign.center),
            ],
            if (actionLabel != null && onAction != null) ...[
              const SizedBox(height: 18),
              FilledButton.icon(onPressed: onAction, icon: const Icon(Icons.add_rounded), label: Text(actionLabel!)),
            ],
          ],
        ),
      ),
    );
  }
}

class SectionHeader extends StatelessWidget {
  const SectionHeader({super.key, required this.title, this.trailing, this.padding});
  final String title;
  final Widget? trailing;
  final EdgeInsets? padding;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: padding ?? const EdgeInsets.fromLTRB(4, 20, 4, 10),
      child: Row(children: [
        Expanded(child: Text(title, style: Theme.of(context).textTheme.titleMedium)),
        ?trailing,
      ]),
    );
  }
}

/// Small rounded status label.
class Pill extends StatelessWidget {
  const Pill({super.key, required this.label, required this.color, required this.background, this.icon});
  final String label;
  final Color color;
  final Color background;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(color: background, borderRadius: BorderRadius.circular(8)),
      child: Row(mainAxisSize: MainAxisSize.min, children: [
        if (icon != null) ...[Icon(icon, size: 13, color: color), const SizedBox(width: 4)],
        Text(label, style: TextStyle(color: color, fontSize: 12, fontWeight: FontWeight.w600)),
      ]),
    );
  }
}

class IconBadge extends StatelessWidget {
  const IconBadge({super.key, required this.icon, required this.color, this.size = 40});
  final IconData icon;
  final Color color;
  final double size;

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: color.withValues(alpha: dark ? 0.22 : 0.12),
        borderRadius: BorderRadius.circular(size * 0.32),
      ),
      child: Icon(icon, size: size * 0.5, color: dark ? Color.lerp(color, Colors.white, 0.45) : color),
    );
  }
}

/// Card with consistent padding and tap feedback.
class AppCard extends StatelessWidget {
  const AppCard({super.key, required this.child, this.onTap, this.padding = const EdgeInsets.all(16), this.color});
  final Widget child;
  final VoidCallback? onTap;
  final EdgeInsets padding;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    return Card(
      color: color,
      clipBehavior: Clip.antiAlias,
      child: InkWell(onTap: onTap, child: Padding(padding: padding, child: child)),
    );
  }
}

class SearchField extends StatelessWidget {
  const SearchField({super.key, required this.controller, required this.hint, required this.onChanged, this.autofocus = false});
  final TextEditingController controller;
  final String hint;
  final ValueChanged<String> onChanged;
  final bool autofocus;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      autofocus: autofocus,
      onChanged: onChanged,
      textInputAction: TextInputAction.search,
      decoration: InputDecoration(
        hintText: hint,
        prefixIcon: const Icon(Icons.search_rounded),
        suffixIcon: ValueListenableBuilder(
          valueListenable: controller,
          builder: (context, v, _) => v.text.isEmpty
              ? const SizedBox.shrink()
              : IconButton(
                  tooltip: context.tr('ล้าง', 'Clear'),
                  icon: const Icon(Icons.close_rounded),
                  onPressed: () {
                    controller.clear();
                    onChanged('');
                  },
                ),
        ),
      ),
    );
  }
}

/// Colours for task priority.
Color priorityColor(BuildContext context, int index) {
  final s = context.status;
  switch (index) {
    case 2:
      return s.danger;
    case 0:
      return Theme.of(context).colorScheme.secondary;
    default:
      return s.warning;
  }
}
