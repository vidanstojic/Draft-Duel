import SwiftUI
import UIKit
import ComposeApp

/// Hosts the shared Compose UI. All screens live in composeApp/src/commonMain.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // Compose handles safe areas itself (WindowInsets.safeDrawing in App.kt).
        ComposeView()
            .ignoresSafeArea(.all)
    }
}
