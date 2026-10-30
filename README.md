<div align="center">
  <img width="180" height="180" src="/preview/logo.svg">
  <h1>Redomi</h1>
  <p>Open and share music links across streaming services, made in Jetpack Compose and Material You</p>
  
  ![Supported versions](https://img.shields.io/badge/Support-8%2B-green?logo=android)
  [![Download count](https://img.shields.io/github/downloads/acszo/Redomi/total?label=Downloads&logo=github)](https://github.com/acszo/Redomi/releases/latest/)
  [![Crowdin](https://badges.crowdin.net/redomi/localized.svg)](https://crowdin.com/project/redomi)

  [<img src="https://github.com/machiav3lli/oandbackupx/blob/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png" alt="Get it on GitHub" height="80">](https://github.com/acszo/Redomi/releases/latest)
  [<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="80">](https://f-droid.org/en/packages/com.acszo.redomi.repo)
  [<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" height="80">](https://apt.izzysoft.de/fdroid/index/apk/com.acszo.redomi.repo)
  
</div>

## Description

Don't you hate when you receive music links from friends who use different platforms than you, and you can't tap and play them in your favourite music service???? <br/>
This app is here to solve that problem! <br/>
Redomi catches those links, lets you open them in the app you pick, and helps you share them in a form anyone can open. <br/> <br/>
<b> All of this is done like it's integrated into your system! </b>

## Features

- Open supported links from different music services in your favourite streaming app
- Convert YouTube and YouTube Music video links directly, keeping the start time
- Share a universal [MusicLink](https://musiclink.one/) link that lets anyone find the song on their preferred platform
- Clean Links (optional): remove known tracking parameters from the links you open, copy or share
- No account, API key or backend needed: links are parsed on your device
- Customize Sharesheet in different ways:
  - Icon shapes
  - Choose which apps to display
  - Decide between a horizontal or vertical list, and its grid size
- Material You
- Light/Dark Theme

## How conversion works

Redomi reads the link locally and shows what it can offer without looking anything up:

- **Original**: open the link as it is, in the service it came from
- **Same ID**: YouTube and YouTube Music share video IDs, so a link can be reopened on the other service. This is not a verified match and the video may not be playable there
- **MusicLink**: opens a [MusicLink](https://musiclink.one/) page that finds the song on other services

Redomi does not search for the same song on other platforms, so it never claims a verified cross-platform match by itself. Links from services other than YouTube and YouTube Music can be opened as they are or sent through MusicLink.

## In action 😎

<p align="center">
  <img src="/preview/open.gif" width="30%">
  <img src="/preview/share.gif" width="30%">
</p>

## Screenshots

<p align="center">
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot_1.png" width="30%" />
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot_2.png" width="30%" />
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot_3.png" width="30%" />
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot_4.png" width="30%" />
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot_5.png" width="30%" />
  <img src="/preview/screenshot_6.png" width="30%" />
</p>

## HOW TO MAKE IT WORK (on Android 12+)

By default Redomi is unable to open links, do as follows:
1. Long-press on Redomi and open its info page
2. Navigate to "Set/Open by default" (this may have a different naming on some devices)
3. Check all the music platforms that are *NOT* installed

## Supported services (+ 3rd party apps)

- Youtube music
- Youtube
- Spotify
- Deezer
- Tidal
- Amazon Music
- Apple Music
- Soundcloud
- Napster
- Yandex Music
- Anghami

3rd party clients are also compatible, as long as they're the default app to handle the corresponding link

## Translations

You can contribute by translating via [Crowdin](https://crowdin.com/project/redomi)

## Credits

Redomi was originally built by [acszo](https://github.com/acszo/Redomi) as a client for the Song.link API, which this fork no longer uses. Thanks to [Ty3uK/songlink-android](https://github.com/Ty3uK/songlink-android) for the original inspiration, and to [MusicLink](https://musiclink.one/) for the universal link page.

Licensed under the GPL, see [LICENSE](LICENSE).
