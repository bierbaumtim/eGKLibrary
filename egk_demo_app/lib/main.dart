import 'package:egk_demo_app/egk_service.dart';
import 'package:flutter/material.dart';

import 'package:egk_demo_app/egk_reader_screen.dart';

import 'storage_screen.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'eGK Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      darkTheme: ThemeData.dark(useMaterial3: true),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;
  late final EGKService _egkService;

  @override
  void initState() {
    super.initState();
    _egkService = EGKService();
    _tabController = TabController(length: 2, vsync: this);

    _egkService.init();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('eGK Demo Home Page'),
        bottom: TabBar(
          controller: _tabController,
          tabs: [
            Tab(text: 'eGK Reader'),
            Tab(text: 'Stored Data'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          EgkReaderScreen(egkService: _egkService),
          StorageScreen(),
        ],
      ),
    );
  }
}
