import SwiftUI
import Shared

@main
struct iOSApp: App {

    init() {
        IosKoinKt.doInitKoinForIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
