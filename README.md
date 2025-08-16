Developed a feature-rich Android translator app in Kotlin using Google ML Kit for live translation/language ID, and Room DB for persistent history. Integrated Android's native speech recognition and Text-to-Speech engines to provide hands-free voice input and audio playback for pronunciation. Designed and built a polished user interface with Material Design 3, featuring an animated and date-grouped history list using a complex RecyclerView.

1. Core Technologies & APIs
  ->Kotlin: The primary, modern programming language used for all app logic.

  ->Google ML Kit: The core machine learning framework for the app's intelligent features.

      a.Translation API: Used for translating text between different languages.

      b.Language ID API: Used to automatically detect the source language from user input.

  ->Android Speech Services:

    a.Speech-to-Text (RecognizerIntent): For capturing voice input through the microphone.

    b.Text-to-Speech Engine: For reading the translated text aloud to help with pronunciation.

2. Architecture & Data Persistence
Room Database: A modern database library from Android Jetpack, used to create and manage a local SQL database for storing the translation history persistently on the device.

   ->Kotlin Coroutines (lifecycleScope): Used for managing background threads to ensure that database operations and other long-running tasks don't freeze the user interface.

3. User Interface & Experience (UI/UX)
      ->Material Design 3: The design system used for all UI components, ensuring a modern and clean look with elements like  MaterialToolbar, TextInputLayout, and ExtendedFloatingActionButton.

  ->ConstraintLayout: Used to build flexible and responsive screen layouts that adapt to different screen sizes.

  ->RecyclerView: The core component for displaying the scrollable translation history list efficiently, capable of handling a large number of items without performance issues.

  ->ItemTouchHelper: Implemented to add the intuitive swipe-to-delete functionality to the history list.

  ->XML Animations: Simple fade-in animations were used to make the appearance of translated results feel smoother and more polished.
