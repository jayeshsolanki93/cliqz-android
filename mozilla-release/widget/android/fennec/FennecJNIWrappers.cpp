// GENERATED CODE
// Generated by the Java program at /build/annotationProcessors at compile time
// from annotations on Java methods. To update, change the annotations on the
// corresponding Java methods and rerun the build. Manually updating this file
// will cause your build to fail.

#ifndef MOZ_PREPROCESSOR
#include "FennecJNIWrappers.h"
#include "mozilla/jni/Accessors.h"
#endif

namespace mozilla {
namespace java {

const char ANRReporter::name[] =
        "org/mozilla/gecko/ANRReporter";

constexpr char ANRReporter::GetNativeStack_t::name[];
constexpr char ANRReporter::GetNativeStack_t::signature[];

constexpr char ANRReporter::ReleaseNativeStack_t::name[];
constexpr char ANRReporter::ReleaseNativeStack_t::signature[];

constexpr char ANRReporter::RequestNativeStack_t::name[];
constexpr char ANRReporter::RequestNativeStack_t::signature[];

const char BrowserLocaleManager::name[] =
        "org/mozilla/gecko/BrowserLocaleManager";

constexpr char BrowserLocaleManager::GetLocale_t::name[];
constexpr char BrowserLocaleManager::GetLocale_t::signature[];

auto BrowserLocaleManager::GetLocale() -> mozilla::jni::String::LocalRef
{
    return mozilla::jni::Method<GetLocale_t>::Call(BrowserLocaleManager::Context(), nullptr);
}

constexpr char BrowserLocaleManager::RefreshLocales_t::name[];
constexpr char BrowserLocaleManager::RefreshLocales_t::signature[];

const char DownloadsIntegration::name[] =
        "org/mozilla/gecko/DownloadsIntegration";

constexpr char DownloadsIntegration::GetTemporaryDownloadDirectory_t::name[];
constexpr char DownloadsIntegration::GetTemporaryDownloadDirectory_t::signature[];

auto DownloadsIntegration::GetTemporaryDownloadDirectory() -> mozilla::jni::String::LocalRef
{
    return mozilla::jni::Method<GetTemporaryDownloadDirectory_t>::Call(DownloadsIntegration::Context(), nullptr);
}

constexpr char DownloadsIntegration::ScanMedia_t::name[];
constexpr char DownloadsIntegration::ScanMedia_t::signature[];

auto DownloadsIntegration::ScanMedia(mozilla::jni::String::Param a0, mozilla::jni::String::Param a1) -> void
{
    return mozilla::jni::Method<ScanMedia_t>::Call(DownloadsIntegration::Context(), nullptr, a0, a1);
}

const char GeckoApp::name[] =
        "org/mozilla/gecko/GeckoApp";

constexpr char GeckoApp::LaunchOrBringToFront_t::name[];
constexpr char GeckoApp::LaunchOrBringToFront_t::signature[];

auto GeckoApp::LaunchOrBringToFront() -> void
{
    return mozilla::jni::Method<LaunchOrBringToFront_t>::Call(GeckoApp::Context(), nullptr);
}

const char GeckoApplication::name[] =
        "org/mozilla/gecko/GeckoApplication";

constexpr char GeckoApplication::CreateShortcut_t::name[];
constexpr char GeckoApplication::CreateShortcut_t::signature[];

auto GeckoApplication::CreateShortcut(mozilla::jni::String::Param a0, mozilla::jni::String::Param a1) -> void
{
    return mozilla::jni::Method<CreateShortcut_t>::Call(GeckoApplication::Context(), nullptr, a0, a1);
}

const char GeckoJavaSampler::name[] =
        "org/mozilla/gecko/GeckoJavaSampler";

constexpr char GeckoJavaSampler::GetFrameName_t::name[];
constexpr char GeckoJavaSampler::GetFrameName_t::signature[];

auto GeckoJavaSampler::GetFrameName(int32_t a0, int32_t a1, int32_t a2) -> mozilla::jni::String::LocalRef
{
    return mozilla::jni::Method<GetFrameName_t>::Call(GeckoJavaSampler::Context(), nullptr, a0, a1, a2);
}

constexpr char GeckoJavaSampler::GetProfilerTime_t::name[];
constexpr char GeckoJavaSampler::GetProfilerTime_t::signature[];

constexpr char GeckoJavaSampler::GetSampleTime_t::name[];
constexpr char GeckoJavaSampler::GetSampleTime_t::signature[];

auto GeckoJavaSampler::GetSampleTime(int32_t a0, int32_t a1) -> double
{
    return mozilla::jni::Method<GetSampleTime_t>::Call(GeckoJavaSampler::Context(), nullptr, a0, a1);
}

constexpr char GeckoJavaSampler::GetThreadName_t::name[];
constexpr char GeckoJavaSampler::GetThreadName_t::signature[];

auto GeckoJavaSampler::GetThreadName(int32_t a0) -> mozilla::jni::String::LocalRef
{
    return mozilla::jni::Method<GetThreadName_t>::Call(GeckoJavaSampler::Context(), nullptr, a0);
}

constexpr char GeckoJavaSampler::Pause_t::name[];
constexpr char GeckoJavaSampler::Pause_t::signature[];

auto GeckoJavaSampler::Pause() -> void
{
    return mozilla::jni::Method<Pause_t>::Call(GeckoJavaSampler::Context(), nullptr);
}

constexpr char GeckoJavaSampler::Start_t::name[];
constexpr char GeckoJavaSampler::Start_t::signature[];

auto GeckoJavaSampler::Start(int32_t a0, int32_t a1) -> void
{
    return mozilla::jni::Method<Start_t>::Call(GeckoJavaSampler::Context(), nullptr, a0, a1);
}

constexpr char GeckoJavaSampler::Stop_t::name[];
constexpr char GeckoJavaSampler::Stop_t::signature[];

auto GeckoJavaSampler::Stop() -> void
{
    return mozilla::jni::Method<Stop_t>::Call(GeckoJavaSampler::Context(), nullptr);
}

constexpr char GeckoJavaSampler::Unpause_t::name[];
constexpr char GeckoJavaSampler::Unpause_t::signature[];

auto GeckoJavaSampler::Unpause() -> void
{
    return mozilla::jni::Method<Unpause_t>::Call(GeckoJavaSampler::Context(), nullptr);
}

const char GlobalHistory::name[] =
        "org/mozilla/gecko/GlobalHistory";

constexpr char GlobalHistory::CheckURIVisited_t::name[];
constexpr char GlobalHistory::CheckURIVisited_t::signature[];

auto GlobalHistory::CheckURIVisited(mozilla::jni::String::Param a0) -> void
{
    return mozilla::jni::Method<CheckURIVisited_t>::Call(GlobalHistory::Context(), nullptr, a0);
}

constexpr char GlobalHistory::MarkURIVisited_t::name[];
constexpr char GlobalHistory::MarkURIVisited_t::signature[];

auto GlobalHistory::MarkURIVisited(mozilla::jni::String::Param a0) -> void
{
    return mozilla::jni::Method<MarkURIVisited_t>::Call(GlobalHistory::Context(), nullptr, a0);
}

constexpr char GlobalHistory::SetURITitle_t::name[];
constexpr char GlobalHistory::SetURITitle_t::signature[];

auto GlobalHistory::SetURITitle(mozilla::jni::String::Param a0, mozilla::jni::String::Param a1) -> void
{
    return mozilla::jni::Method<SetURITitle_t>::Call(GlobalHistory::Context(), nullptr, a0, a1);
}

const char MemoryMonitor::name[] =
        "org/mozilla/gecko/MemoryMonitor";

constexpr char MemoryMonitor::DispatchMemoryPressure_t::name[];
constexpr char MemoryMonitor::DispatchMemoryPressure_t::signature[];

const char PresentationMediaPlayerManager::name[] =
        "org/mozilla/gecko/PresentationMediaPlayerManager";

constexpr char PresentationMediaPlayerManager::AddPresentationSurface_t::name[];
constexpr char PresentationMediaPlayerManager::AddPresentationSurface_t::signature[];

constexpr char PresentationMediaPlayerManager::InvalidateAndScheduleComposite_t::name[];
constexpr char PresentationMediaPlayerManager::InvalidateAndScheduleComposite_t::signature[];

constexpr char PresentationMediaPlayerManager::RemovePresentationSurface_t::name[];
constexpr char PresentationMediaPlayerManager::RemovePresentationSurface_t::signature[];

const char Telemetry::name[] =
        "org/mozilla/gecko/Telemetry";

constexpr char Telemetry::AddHistogram_t::name[];
constexpr char Telemetry::AddHistogram_t::signature[];

constexpr char Telemetry::AddKeyedHistogram_t::name[];
constexpr char Telemetry::AddKeyedHistogram_t::signature[];

constexpr char Telemetry::AddUIEvent_t::name[];
constexpr char Telemetry::AddUIEvent_t::signature[];

constexpr char Telemetry::StartUISession_t::name[];
constexpr char Telemetry::StartUISession_t::signature[];

constexpr char Telemetry::StopUISession_t::name[];
constexpr char Telemetry::StopUISession_t::signature[];

const char ThumbnailHelper::name[] =
        "org/mozilla/gecko/ThumbnailHelper";

constexpr char ThumbnailHelper::NotifyThumbnail_t::name[];
constexpr char ThumbnailHelper::NotifyThumbnail_t::signature[];

auto ThumbnailHelper::NotifyThumbnail(mozilla::jni::ByteBuffer::Param a0, mozilla::jni::Object::Param a1, bool a2, bool a3) -> void
{
    return mozilla::jni::Method<NotifyThumbnail_t>::Call(ThumbnailHelper::Context(), nullptr, a0, a1, a2, a3);
}

constexpr char ThumbnailHelper::RequestThumbnail_t::name[];
constexpr char ThumbnailHelper::RequestThumbnail_t::signature[];

const char AudioFocusAgent::name[] =
        "org/mozilla/gecko/media/AudioFocusAgent";

constexpr char AudioFocusAgent::NotifyStartedPlaying_t::name[];
constexpr char AudioFocusAgent::NotifyStartedPlaying_t::signature[];

auto AudioFocusAgent::NotifyStartedPlaying() -> void
{
    return mozilla::jni::Method<NotifyStartedPlaying_t>::Call(AudioFocusAgent::Context(), nullptr);
}

constexpr char AudioFocusAgent::NotifyStoppedPlaying_t::name[];
constexpr char AudioFocusAgent::NotifyStoppedPlaying_t::signature[];

auto AudioFocusAgent::NotifyStoppedPlaying() -> void
{
    return mozilla::jni::Method<NotifyStoppedPlaying_t>::Call(AudioFocusAgent::Context(), nullptr);
}

const char Restrictions::name[] =
        "org/mozilla/gecko/restrictions/Restrictions";

constexpr char Restrictions::IsAllowed_t::name[];
constexpr char Restrictions::IsAllowed_t::signature[];

auto Restrictions::IsAllowed(int32_t a0, mozilla::jni::String::Param a1) -> bool
{
    return mozilla::jni::Method<IsAllowed_t>::Call(Restrictions::Context(), nullptr, a0, a1);
}

constexpr char Restrictions::IsUserRestricted_t::name[];
constexpr char Restrictions::IsUserRestricted_t::signature[];

auto Restrictions::IsUserRestricted() -> bool
{
    return mozilla::jni::Method<IsUserRestricted_t>::Call(Restrictions::Context(), nullptr);
}

} /* java */
} /* mozilla */