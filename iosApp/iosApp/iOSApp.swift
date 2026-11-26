import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        let baseUrl = Bundle.main.object(forInfoDictionaryKey: "BACKEND_BASE_URL") as? String ?? "http://localhost:8000/"
        MainViewControllerKt.configureApp(baseUrl: baseUrl, isDebug: isDebugBuild)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

#if DEBUG
private let isDebugBuild = true
#else
private let isDebugBuild = false
#endif