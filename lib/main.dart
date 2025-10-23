import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(const EagleEye());

class EagleEye extends StatefulWidget {
  const EagleEye({super.key});

  @override
  State<EagleEye> createState() => _EagleEyeState();
}

class _EagleEyeState extends State<EagleEye> {
  static const platform = MethodChannel('eagle_eye_listener');
  String lastDetected = 'Welcome to Eagle Eye 🦅';

  @override
  void initState() {
    super.initState();
    _initListener();
  }

  void _initListener() {
    platform.setMethodCallHandler((call) async {
      var method = call.method;
      var data = call.arguments;

      if (method == 'eagle.media') {
        // setState(() {
        //   lastDetected = 'New file: ${call.arguments}';
        // });

        if(data != null) {

          //we have a file/path instance
          // print(data);

        }
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: true,
      title: 'Eagle Eye',
      home: Scaffold(
        body: Center(
          child: Text(lastDetected, textAlign: TextAlign.center),
        ),
      ),
      builder: (context, child) {

        return child!;

      },
    );
  }
}