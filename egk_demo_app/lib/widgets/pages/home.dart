import 'package:flutter/material.dart';

import '../../services/egk_service.dart';
import 'egk_reader_screen.dart';
import 'storage_screen.dart';

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
