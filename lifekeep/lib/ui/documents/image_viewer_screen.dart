import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../state/app_controller.dart';

/// Full-screen, zoomable viewer. Images are decoded at roughly screen
/// resolution and only the visible page is in memory.
class ImageViewerScreen extends StatefulWidget {
  const ImageViewerScreen({super.key, required this.names, this.initialIndex = 0});
  final List<String> names;
  final int initialIndex;

  @override
  State<ImageViewerScreen> createState() => _ImageViewerScreenState();
}

class _ImageViewerScreenState extends State<ImageViewerScreen> {
  late final PageController _pc = PageController(initialPage: widget.initialIndex);
  late int _index = widget.initialIndex;

  @override
  void dispose() {
    _pc.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final c = context.read<AppController>();
    final size = MediaQuery.sizeOf(context);
    final dpr = MediaQuery.devicePixelRatioOf(context);
    final decodeWidth = (size.width * dpr * 2).round().clamp(800, 4096);
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
        title: Text('${_index + 1} / ${widget.names.length}', style: const TextStyle(color: Colors.white)),
      ),
      body: PageView.builder(
        controller: _pc,
        itemCount: widget.names.length,
        onPageChanged: (i) => setState(() => _index = i),
        itemBuilder: (context, i) {
          Widget missing() => Center(
                child: Column(mainAxisSize: MainAxisSize.min, children: [
                  const Icon(Icons.broken_image_outlined, color: Colors.white70, size: 56),
                  const SizedBox(height: 12),
                  Text(
                    context.tr('ไม่พบไฟล์รูปภาพ ไฟล์อาจถูกย้ายหรือลบ', 'Image file not found. It may have been moved or deleted.'),
                    style: const TextStyle(color: Colors.white70),
                    textAlign: TextAlign.center,
                  ),
                ]),
              );
          try {
            final f = c.s.attachments.fileFor(widget.names[i]);
            return InteractiveViewer(
              minScale: 1,
              maxScale: 6,
              child: Center(
                child: Image.file(
                  f,
                  fit: BoxFit.contain,
                  cacheWidth: decodeWidth,
                  errorBuilder: (context, e, s) => missing(),
                ),
              ),
            );
          } catch (_) {
            return missing();
          }
        },
      ),
    );
  }
}
