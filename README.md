# MAL Image Downloader

A powerful Android app that downloads and organizes images from MyAnimeList (MAL) XML export files with advanced metadata embedding and AVES gallery integration.

## 🌟 Features

### Core Functionality
- **MAL API Integration**: Uses your MAL Client ID with secondary API fallback for redundancy
- **XML Parser**: Processes MAL XML export files to extract anime and manga data
- **Background Downloads**: Reliable image downloading with WorkManager and progress tracking
- **Metadata Embedding**: Complete MAL data embedded in images using EXIF/XMP for AVES compatibility
- **Smart Organization**: Automatic folder structure based on genres and content types

### Folder Structure
```
/MAL/
├── ANIME/
│   ├── Action/
│   ├── Romance/
│   ├── Sci-Fi/
│   └── HENTAI/
│       ├── Vanilla/
│       ├── NTR/
│       ├── Incest/
│       ├── Mother-Son/
│       └── Other/
└── MANGA/
    ├── Action/
    ├── Drama/
    ├── Horror/
    └── HENTAI/
        ├── Vanilla/
        ├── NTR/
        ├── Romance/
        └── Other/
```

### Advanced Features
- **Command Interface**: CLI-style power user interface with batch operations
- **AVES Gallery Integration**: Images tagged with metadata that AVES can read and index
- **Parental Controls**: PIN-protected content filtering with age verification
- **Privacy Protection**: `.nomedia` files for adult content folders
- **Progress Monitoring**: Real-time download progress and completion tracking

## 🚀 Quick Start

### Prerequisites
- Android 7.0 (API level 24) or higher
- MyAnimeList Client ID ([Get one here](https://myanimelist.net/apiconfig))
- MAL XML export file from your anime/manga list

### Installation
1. Clone this repository
2. Open in Android Studio
3. Build and install on your device
4. Configure your MAL Client ID in settings

### Basic Usage

#### Command Interface
The app features a powerful command interface for batch operations:

```
# Load your MAL XML export
load-xml /storage/emulated/0/Download/mal_export.xml

# Download all images
download-all

# Check progress
status

# Organize by type
organize anime
organize manga

# Configure settings
settings set mal_client_id=YOUR_CLIENT_ID
settings set secondary_api_url=https://your-backup-api.com
```

#### Available Commands
- `load-xml <path>` - Load MAL XML export file
- `download-all` - Start downloading all loaded images
- `organize anime|manga` - Organize images by type
- `status` - Show download and organization status
- `settings show|set key=value` - Manage app configuration
- `help` - Show command help
- `clear` - Clear loaded entries
- `count` - Show count of loaded entries

## 🛠️ Technical Architecture

### Core Components

#### MAL API Client (`MALApiClient.kt`)
- Primary MAL API integration with Client ID authentication
- Secondary API fallback for redundancy
- Retry logic and error handling

#### XML Parser (`MALXmlParser.kt`)
- Parses MAL XML export files
- Extracts anime/manga metadata including titles, genres, images
- Data models for structured information

#### Download System
- **`ImageDownloadWorker.kt`**: Background download worker using WorkManager
- **`DownloadManager.kt`**: Batch download management and queue control
- Network constraints (WiFi-only option) and battery optimization

#### Metadata & Organization
- **`MetadataEmbedder.kt`**: EXIF/XMP metadata embedding for AVES compatibility
- **`FolderOrganizer.kt`**: Smart folder organization with genre detection
- Adult content classification and privacy controls

#### User Interface
- **Jetpack Compose**: Modern Android UI with Material Design 3
- **Navigation**: Bottom navigation between Command, Progress, and Settings
- **ViewModels**: MVVM architecture for state management

#### Safety & Controls
- **`ParentalControlManager.kt`**: PIN-protected content filtering
- **`AgeVerificationDialog.kt`**: Age verification for adult content
- Content rating system (PG, PG-13, R, X, XXX)

## 📱 Screenshots & UI

### Command Interface
Powerful CLI-style interface with:
- Command history and scrolling
- Auto-completion suggestions
- Real-time feedback and error handling
- Quick action buttons for common commands

### Download Progress
Real-time monitoring with:
- Individual download progress bars
- Overall statistics and success rates
- Cancel and retry functionality
- Queue management controls

### Settings & Parental Controls
Comprehensive settings including:
- MAL Client ID configuration
- Download preferences (WiFi-only, charging requirements)
- Parental controls with PIN protection
- Content filtering and age verification

## 🔐 Privacy & Security

### Adult Content Protection
- Age verification required for 18+ content
- PIN-protected parental controls
- Content rating enforcement
- Hidden folders with `.nomedia` files

### Data Privacy
- All data stored locally on device
- No personal information shared with third parties
- Birth date used only for age verification
- MAL Client ID stored securely in app preferences

## 🔧 Configuration

### API Setup
1. Get your MAL Client ID from [MyAnimeList API Config](https://myanimelist.net/apiconfig)
2. Open app settings and enter your Client ID
3. Optionally configure a secondary API URL for redundancy

### Export Your MAL Data
1. Go to your [MAL profile settings](https://myanimelist.net/editprofile.php)
2. Navigate to "Import/Export" section
3. Export your anime and manga lists as XML
4. Save the XML file to your device storage

### Download Preferences
- **WiFi Only**: Restrict downloads to WiFi connections
- **Require Charging**: Only download when device is charging
- **Parental Controls**: Set content filters and age restrictions

## 🎯 AVES Gallery Integration

Images are tagged with comprehensive metadata that AVES gallery can read:

### EXIF Tags
- `ImageDescription`: Anime/manga title
- `Artist`: Media type (TV Anime, Manga, etc.)
- `Copyright`: Genre information
- `UserComment`: Complete MAL data in JSON format

### XMP Tags
- `dc:title`: Title (Dublin Core standard)
- `dc:subject`: Genres and tags
- `dc:type`: Media type classification
- `mal:id`: MAL database ID
- Custom namespace for MAL-specific fields

### Virtual GPS Coordinates
AVES can group images by location, so we use virtual coordinates based on genre/type for organization.

## 📋 Requirements

### System Requirements
- Android 7.0 (API level 24) or higher
- 2GB RAM minimum (4GB recommended)
- Storage space for downloaded images
- Internet connection for downloading

### Permissions
- `INTERNET`: Download images from web
- `WRITE_EXTERNAL_STORAGE`: Save images to storage
- `FOREGROUND_SERVICE`: Background downloads
- `POST_NOTIFICATIONS`: Download progress notifications

## 🤝 Contributing

### Development Setup
1. Clone the repository
2. Open in Android Studio Arctic Fox or newer
3. Sync Gradle dependencies
4. Run on device or emulator

### Code Structure
```
app/src/main/kotlin/com/osphvdhwj/malimage/
├── api/           # API clients and network layer
├── cli/           # Command line interface
├── download/      # Download management
├── metadata/      # EXIF/XMP metadata handling
├── parser/        # XML parsing and data models
├── safety/        # Parental controls and age verification
├── storage/       # File organization and folder management
├── ui/            # User interface components
│   ├── theme/     # Material Design 3 theme
│   └── *.kt       # Compose screens and components
└── viewmodel/     # State management
```

### Commit Guidelines
- Use descriptive commit messages
- Include module name in commits (e.g., "Add MetadataEmbedder with EXIF support")
- Test changes thoroughly before committing
- Document new features and API changes

## 📚 Documentation

### API Documentation
- [MyAnimeList API v2](https://myanimelist.net/apiconfig/references/api/v2)
- [AVES Gallery Metadata](https://github.com/deckerst/aves)
- [Android WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [ExifInterface](https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface)

## 🐛 Issues & Support

### Known Issues
- Some MAL API endpoints may have rate limits
- Large batch downloads may take significant time
- Adult content detection relies on genre tags and may not catch all content

### Reporting Bugs
1. Check existing issues first
2. Provide detailed reproduction steps
3. Include app version and Android version
4. Attach relevant logs if possible

## 📄 License

This project is open source. Feel free to use, modify, and distribute according to your needs.

## 🙏 Acknowledgments

- [MyAnimeList](https://myanimelist.net) for providing the API and data
- [AVES Gallery](https://github.com/deckerst/aves) for inspiration on metadata standards
- Android community for excellent libraries and documentation
- Contributors and testers who help improve the app

## 🔮 Roadmap

### Planned Features
- [ ] Batch processing for multiple XML files
- [ ] Cloud storage backup integration
- [ ] AI-powered content classification
- [ ] Social features for sharing collections
- [ ] Custom theme support
- [ ] Export functionality to other formats
- [ ] Advanced search and filtering
- [ ] Duplicate detection and management

### Performance Improvements
- [ ] Database integration for faster searches
- [ ] Image compression options
- [ ] Progressive loading for large collections
- [ ] Offline mode capabilities

---

**Made with ❤️ for the anime and manga community**

*This app is not affiliated with MyAnimeList. MAL is a trademark of MyAnimeList Co., Ltd.*