#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint egk_demo_app_native.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'egk_demo_app_native'
  s.version          = '0.0.1'
  s.summary          = 'Flutter plugin for reading German electronic health card (eGK) data via NFC.'
  s.description      = <<-DESC
A Flutter plugin for reading data from German electronic health cards (eGK) using NFC.
Supports reading personal and insurance data.
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '13.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'
  
  # OpenHealthCardKit dependencies
  # s.dependency 'CardReaderProviderApi', '~> 5.6'
  # s.dependency 'HealthCardAccess', '~> 5.6'
  # s.dependency 'HealthCardControl', '~> 5.6'
  # s.dependency 'NFCCardReaderProvider', '~> 5.6'
  
  # Gzip decompression
  # s.dependency 'GzipSwift', '~> 6.0'
  
  # NFC requires CoreNFC framework, zlib is always available
  s.frameworks = 'CoreNFC'
  s.libraries = 'z'

  # If your plugin requires a privacy manifest, for example if it uses any
  # required reason APIs, update the PrivacyInfo.xcprivacy file to describe your
  # plugin's privacy impact, and then uncomment this line. For more information,
  # see https://developer.apple.com/documentation/bundleresources/privacy_manifest_files
  # s.resource_bundles = {'egk_demo_app_native_privacy' => ['Resources/PrivacyInfo.xcprivacy']}
end
