"""
Turns a learning resource into the text its questions can actually be based on.

Until now the question generator only knew a topic name and a video title, which
is not enough to write a question about what was actually said. This module
reaches for the real content:

* a YouTube video contributes its captions, trimmed to the point the student
  had reached when they pressed "I have learned up to here";
* a documentation page contributes its readable text, with navigation, scripts
  and boilerplate stripped out.

Both are best effort. Plenty of videos have no captions and plenty of sites
refuse automated requests, so a failure here returns ``available: False`` with a
reason instead of raising, and the caller carries on asking about the topic on
its own.
"""

import logging
import os
import re
import threading
from urllib.parse import parse_qs, urlparse

logger = logging.getLogger("content_extractor")

# Only these hosts are ever fetched. The Spring service applies the same list
# before it stores a URL, so this endpoint cannot be used to make the service
# call an arbitrary address. A host matches on its own name or as a subdomain of
# an entry, which covers docs.aws.amazon.com and en.wikipedia.org without
# opening the door to those domains.
ALLOWED_HOSTS = {
    "youtube.com", "youtu.be",
    "w3schools.com", "developer.mozilla.org", "wikipedia.org", "google.com",
    "geeksforgeeks.org", "javatpoint.com", "tutorialspoint.com", "coursera.org",
    "docs.oracle.com", "dev.java", "learn.microsoft.com", "php.net", "python.org",
    "kotlinlang.org", "swift.org", "ruby-lang.org", "scala-lang.org",
    "cppreference.com", "cplusplus.com", "gnu.org", "nasm.us", "man7.org",
    "kernel.org", "typescriptlang.org", "javascript.info",
    "nodejs.org", "expressjs.com", "django-project.com", "djangoproject.com",
    "palletsprojects.com", "rubyonrails.org", "laravel.com", "spring.io",
    "react.dev", "vuejs.org", "angular.dev", "flutter.dev", "android.com",
    "bootstrap.com", "tailwindcss.com",
    "postgresql.org", "mysql.com", "mongodb.com", "redis.io", "sqlite.org",
    "apache.org", "aws.amazon.com", "cloud.google.com", "azure.microsoft.com",
    "docker.com", "kubernetes.io", "helm.sh", "terraform.io",
    "numpy.org", "scipy.org", "pandas.pydata.org",
    "matplotlib.org", "scikit-learn.org", "pytorch.org", "tensorflow.org",
    "huggingface.co", "kaggle.com", "opencv.org",
    "developers.google.com", "spark.apache.org", "databricks.com",
    "owasp.org", "mitre.org", "nvd.nist.gov",
    "rfc-editor.org", "ietf.org", "w3.org",
    "riscv.org", "visualgo.net", "craftinginterpreters.com",
    "dataintensive.net", "roadmap.sh", "git-scm.com", "github.com",
    "ieee.org", "acm.org", "iso.org", "nist.gov",
    "cmu.edu", "wisc.edu", "mit.edu", "stanford.edu", "berkeley.edu",
    "khanacademy.org", "allaboutcircuits.com", "investopedia.com",
    "accountingcoach.com", "marketingexamples.com", "toastmasters.org",
    "digitalforensicsmagazine.com", "linuxjourney.com", "wooledge.org",
    "programiz.com", "nptel.ac.in", "nvlpubs.nist.gov",
    "r-project.org", "ethereum.org", "isocpp.org", "learnopengl.com", "web.dev",
}

# Cap on how much text goes into the prompt. A long video still yields more than
# enough to write twenty questions from, and an unbounded transcript would blow
# the model's context for no gain.
MAX_TEXT_CHARS = int(os.environ.get("CONTENT_MAX_CHARS", "12000"))

# A page that is mostly navigation produces a worse question set than no page at
# all, so a fetch that yields less than this is treated as a failure.
MIN_USEFUL_CHARS = 200

FETCH_TIMEOUT = float(os.environ.get("CONTENT_FETCH_TIMEOUT", "8"))
MAX_PAGE_BYTES = 1_500_000
USER_AGENT = "SmartLearn/1.0 (student question generation)"

# Auto-generated captions come in overlapping fragments, and the same few words
# are often repeated while a speaker hesitates. Collapsing exact repeats makes
# the transcript far more readable for the model.
_CAPTION_DROP = re.compile(r"[\[\(]\s*(music|applause|laughter|silence|inaudible)\s*[\]\)]", re.I)
_WHITESPACE = re.compile(r"\s+")
_BLOCK_TAGS = ("p", "h1", "h2", "h3", "h4", "h5", "h6", "li", "pre", "code",
               "td", "th", "dt", "dd", "blockquote", "figcaption")
# Chrome that never carries the lesson itself.
_STRIP_TAGS = ("script", "style", "noscript", "nav", "footer", "header", "aside",
               "form", "button", "svg", "iframe", "figure", "figcaption",
               "template", "select", "input", "label")
# Plenty of sites build their navigation out of plain divs and lists rather than
# a <nav>, so the class and id have to be checked too. Left in, a sidebar full
# of link text ends up in the prompt and the model writes questions about the
# site's own menu.
#
# These match a whole hyphen-separated segment or a prefix of the whole class,
# never a fragment in the middle of a word. That distinction matters: Wikipedia
# puts "in-header-enabled" in a feature flag on <html>, and matching "header"
# anywhere in the class deletes the entire article.
_CHROME_WORDS = frozenset((
    "nav", "navbar", "navigation", "sidebar", "sidenav", "menu", "megamenu",
    "breadcrumb", "breadcrumbs", "comment", "comments", "disqus", "advert",
    "adverts", "advertisement", "advertisements", "ad", "ads", "cookie",
    "cookies", "consent", "popup", "popups", "modal", "social", "share",
    "sharing", "related", "trending", "popular", "widget", "widgets",
    "pagination", "pager", "toolbar", "masthead", "banner", "banners",
    "offcanvas", "hamburger", "search", "newsletter", "subscribe",
    "copyright", "footer", "toc", "skip", "dropdown", "pagination-list",
))
_CHROME_PREFIXES = ("nav", "sidenav", "sidebar", "menu", "megamenu", "bread",
                    "comment", "disqus", "advert", "ad-", "ads-", "cookie",
                    "consent", "popup", "modal", "social", "share", "sharing",
                    "related", "trending", "popular", "widget", "paginat",
                    "pager", "toolbar", "masthead", "banner", "skip", "search",
                    "offcanvas", "hamburger", "newsletter", "subscribe",
                    "copyright", "dropdown")
# These usually hold the article when a documentation site has one.
_MAIN_SELECTORS = ("article", "main", "[role=main]", "#content", ".content",
                   ".mw-parser-output", "#mw-content-text", ".markdown-body",
                   ".post-content", ".entry-content", ".article-content",
                   ".page-content", ".docs-content", ".tutorial-content",
                   ".w3-main", ".main-content", "#main-content", ".container")

TRANSCRIPT_LANGUAGES = ("en", "en-US", "en-GB", "en-IN", "hi", "hi-IN")

VIDEO_ID_RE = re.compile(r"^[A-Za-z0-9_-]{11}$")


def _log_once(key: str, message: str) -> None:
    """Extraction failures are expected (no captions, blocked site), so keep them quiet."""
    logger.info("%s: %s", key, message)


def host_allowed(host: str) -> bool:
    """True for a host on the allow list, including any subdomain of one."""
    if not host:
        return False
    value = host.strip().lower()
    return any(value == allowed or value.endswith("." + allowed) for allowed in ALLOWED_HOSTS)


def normalise_url(url: str) -> str:
    """Returns the url if it is a plain http(s) address on a host we allow, else ""."""
    if not url:
        return ""
    value = str(url).strip()
    parsed = urlparse(value)
    if parsed.scheme not in ("http", "https"):
        return ""
    if not host_allowed(parsed.hostname or ""):
        return ""
    return value


def _attribute(tag, name: str) -> str:
    """An attribute as readable text, or "" if the tag has already been removed."""
    if getattr(tag, "decomposed", False) or getattr(tag, "attrs", None) is None:
        return ""
    value = tag.get(name)
    if value is None:
        return ""
    if isinstance(value, (list, tuple)):
        return " ".join(str(part) for part in value)
    return str(value)


def _decompose(tag) -> None:
    if not getattr(tag, "decomposed", False):
        tag.decompose()


def _looks_like_chrome(identifier: str) -> bool:
    """
    True when a class or id names navigation, sharing or adverts rather than content.

    A class is a list of words, and each word is checked whole, so "post-content"
    and "vector-feature-in-header-enabled" both survive.

    "header" is deliberately not treated as a chrome word. Wikipedia puts
    feature flags such as "vector-sticky-header" on <html>, and matching it
    there deletes the whole article. The <header> element is already stripped
    by tag, and the size of a page's header text is a fair price for that.
    """
    if not identifier:
        return False
    for word in re.split(r"[\s_-]+", identifier.lower()):
        if not word:
            continue
        if word in _CHROME_WORDS:
            return True
        if word.startswith(_CHROME_PREFIXES):
            return True
    # Also allow a whole token to lead with a chrome word, which is how
    # "sidebar-primary" and "navBarToggle" are written.
    for token in identifier.lower().split():
        if token.startswith(_CHROME_PREFIXES):
            return True
    return False


def video_id(url: str) -> str:
    """The 11-character id of a specific YouTube video, or "" for anything else."""
    if not url:
        return ""
    parsed = urlparse(str(url).strip())
    host = (parsed.hostname or "").lower().removeprefix("m.")
    if host not in ("youtube.com", "www.youtube.com", "youtu.be", "www.youtu.be"):
        return ""

    candidate = (parse_qs(parsed.query).get("v") or [""])[0]
    parts = [p for p in parsed.path.split("/") if p]
    if not candidate and host.endswith("youtu.be") and parts:
        candidate = parts[0]
    if not candidate and parts and parts[0] in ("shorts", "embed", "live", "v"):
        candidate = parts[1] if len(parts) > 1 else ""
    return candidate if VIDEO_ID_RE.match(candidate or "") else ""


def tidy(text: str) -> str:
    """Collapse whitespace and drop caption noise, without changing the words."""
    if not text:
        return ""
    cleaned = _CAPTION_DROP.sub(" ", str(text))
    return _WHITESPACE.sub(" ", cleaned).strip()


def clip(text: str, max_chars: int = MAX_TEXT_CHARS) -> str:
    """Cuts at a word boundary so the model never receives half a sentence."""
    value = tidy(text)
    if len(value) <= max_chars:
        return value
    cut = value[:max_chars]
    space = cut.rfind(" ")
    return (cut[:space] if space > max_chars // 2 else cut).rstrip() + " ..."


class ContentExtractorService:
    """Pulls readable text out of a video or a page, and caches the raw fetch."""

    def __init__(self):
        # The caches are keyed on the url and hold the *untrimmed* result, so
        # asking again for a different point in the video costs nothing.
        self._lock = threading.Lock()
        self._cue_cache: dict = {}
        self._page_cache: dict = {}

    # ------------------------------------------------------------------ #
    def extract(self, url: str, kind: str = "AUTO", max_seconds=None) -> dict:
        """
        Returns the readable text of a resource.

        `max_seconds` only means something for a video: the transcript is
        trimmed to that point, which is how "I have learned up to here" is
        honoured. A page has no position, so it is always returned whole.
        """
        target = normalise_url(url)
        if not target:
            return self._empty("That link cannot be read.")
        cutoff = self._cutoff(max_seconds)

        vid = video_id(target)
        want_video = vid and str(kind).upper() != "DOC"

        if want_video:
            result = self._from_video(vid, target, cutoff)
            if result["available"]:
                return result
            # A doc url that happens to point at YouTube should still get the
            # page treatment rather than nothing.
            if str(kind).upper() == "VIDEO":
                return result

        return self._from_page(target)

    # ------------------------------------------------------------------ #
    def _from_video(self, vid: str, url: str, cutoff) -> dict:
        cues = self._cues(vid, url)
        if not cues:
            return self._empty("This video has no captions to read, so the questions "
                               "are based on the topic instead.")

        total = max((float(c[0]) + float(c[1] or 0)) for c in cues) if cues else 0.0
        used = [c for c in cues if cutoff is None or float(c[0]) <= cutoff]
        if not used:
            # Stopped before the first line: say so rather than guessing.
            return self._empty("Nothing has been played yet, so there is nothing to "
                               "base questions on. Watch a little of the video first.")
        if not cutoff or cutoff >= total:
            covered = None
            truncated = False
        else:
            covered = int(cutoff)
            truncated = True

        text = clip(" ".join(tidy(c[2]) for c in used))
        if not text:
            return self._empty("The captions for this video were empty.")

        return {
            "available": True,
            "source": "TRANSCRIPT",
            "text": text,
            "duration_seconds": int(total),
            "covered_seconds": covered,
            "truncated": truncated,
            "message": "",
        }

    def _from_page(self, url: str) -> dict:
        text = self._page_text(url)
        if not text:
            return self._empty("This page could not be read, so the questions are "
                               "based on the topic instead.")
        return {
            "available": True,
            "source": "PAGE",
            "text": text,
            "duration_seconds": None,
            "covered_seconds": None,
            "truncated": False,
            "message": "",
        }

    # ------------------------------------------------------------------ #
    def _cues(self, vid: str, url: str) -> list:
        """Fetched captions as (start, duration, text), cached per video."""
        with self._lock:
            if vid in self._cue_cache:
                return self._cue_cache[vid]

        cues = []
        try:
            from youtube_transcript_api import YouTubeTranscriptApi

            api = YouTubeTranscriptApi()
            # 1.x moved to fetch(); 0.6.x only had get_transcript().
            fetched = api.fetch(vid, languages=list(TRANSCRIPT_LANGUAGES))
            raw = getattr(fetched, "snippets", None)
            if raw is not None:
                cues = [(float(s.start), float(getattr(s, "duration", 0) or 0), s.text)
                        for s in raw]
            else:
                cues = [(float(c.get("start", 0)), float(c.get("duration", 0) or 0),
                         c.get("text", "")) for c in fetched]
        except ImportError:
            _log_once(vid, "youtube-transcript-api is not installed")
            return []
        except Exception as exc:  # noqa: BLE001 - no captions, blocked, rate limited
            _log_once(vid, f"captions unavailable ({type(exc).__name__}: {exc})")
            return []

        cues = self._drop_repeats(cues)
        with self._lock:
            self._cue_cache[vid] = cues
        return cues

    @staticmethod
    def _drop_repeats(cues: list) -> list:
        """Auto-captions repeat whole phrases; a line seen twice adds nothing."""
        out = []
        recent = []
        for cue in cues:
            line = tidy(cue[2])
            if not line:
                continue
            if line in recent:
                continue
            recent.append(line)
            if len(recent) > 6:
                recent.pop(0)
            out.append((cue[0], cue[1], line))
        return out

    def _page_text(self, url: str) -> str:
        with self._lock:
            if url in self._page_cache:
                return self._page_cache[url]

        text = self._fetch_page(url)
        with self._lock:
            self._page_cache[url] = text
        return text

    def _fetch_page(self, url: str) -> str:
        try:
            import requests
            from bs4 import BeautifulSoup
        except ImportError as exc:
            _log_once(url, f"page reader dependency missing ({exc})")
            return ""

        try:
            response = requests.get(
                url,
                timeout=FETCH_TIMEOUT,
                headers={"User-Agent": USER_AGENT,
                         "Accept": "text/html,application/xhtml+xml"},
                stream=True,
            )
            if response.status_code >= 400:
                _log_once(url, f"page returned HTTP {response.status_code}")
                return ""
            # A redirect can point anywhere, so it is checked like a fresh url.
            landed = normalise_url(response.url)
            if not landed:
                _log_once(url, f"redirected off the allow list ({response.url})")
                return ""

            body = response.raw.read(MAX_PAGE_BYTES + 1, decode_content=True)
            response.close()
            if not body or len(body) > MAX_PAGE_BYTES:
                _log_once(url, "page was too large to read")
                return ""

            soup = BeautifulSoup(body, "html.parser")
            text = self._readable_text(soup, by_class=True)
            if len(text) < MIN_USEFUL_CHARS:
                # The class and id heuristics are guesses about a layout nobody
                # described to us, and one bad guess can remove the article
                # along with the menu. Retrying without them keeps the lesson,
                # at the cost of some leftover navigation. Worse to guess and
                # lose the content than to keep a page of links.
                text = self._readable_text(
                    BeautifulSoup(body, "html.parser"), by_class=False)
        except Exception as exc:  # noqa: BLE001 - network and parser errors alike
            _log_once(url, f"page could not be fetched ({type(exc).__name__}: {exc})")
            return ""

        if len(text) < MIN_USEFUL_CHARS:
            _log_once(url, f"page yielded only {len(text)} characters of body text")
            return ""
        return clip(text)

    @staticmethod
    def _readable_text(soup, by_class: bool = True) -> str:
        """The lesson, not the furniture around it."""
        # Removing a tag also removes everything inside it, so a tag picked up
        # before its parent was removed is gone by the time we look at it.
        for tag in soup(list(_STRIP_TAGS)):
            _decompose(tag)
        if by_class:
            for tag in soup.find_all(attrs={"class": True}):
                if _looks_like_chrome(_attribute(tag, "class")):
                    _decompose(tag)
            for tag in soup.find_all(attrs={"id": True}):
                if _looks_like_chrome(_attribute(tag, "id")):
                    _decompose(tag)

        root = ContentExtractorService._pick_content(soup) or soup.body or soup
        if not root:
            return ""

        # One line per block element, so paragraphs and list items do not run
        # together into a wall of text.
        lines = []
        for element in root.find_all(_BLOCK_TAGS):
            line = element.get_text(" ", strip=True)
            if line:
                lines.append(line)
        if not lines:
            return tidy(root.get_text(" ", strip=True))
        # Nested tags make a container's text appear several times, so duplicates
        # are dropped while the order is kept.
        seen = set()
        out = []
        for line in lines:
            line = tidy(line)
            if line and line not in seen:
                seen.add(line)
                out.append(line)
        return "\n".join(out)

    @staticmethod
    def _pick_content(soup):
        """
        The element holding the lesson.

        <p>The named selectors are tried first because they are a deliberate
        signal from the site. Failing that, every block container is scored by
        how much text it holds, and the *innermost* of the containers that hold
        almost all of that text wins. Taking the outermost would bring back the
        navigation that sits beside the article, which is exactly what this
        whole method exists to avoid.
        """
        named = []
        for selector in _MAIN_SELECTORS:
            try:
                found = soup.select(selector)
            except Exception:  # noqa: BLE001 - a malformed selector must not abort
                continue
            named.extend(found)
        if named:
            best = max(named, key=lambda el: len(el.get_text(" ", strip=True)))
            if len(best.get_text(" ", strip=True)) >= MIN_USEFUL_CHARS:
                return best

        candidates = soup.find_all(("div", "section", "article", "main"))
        if not candidates:
            return None
        scored = [(len(el.get_text(" ", strip=True)), len(el.find_all(True)), el)
                  for el in candidates]
        longest = max(score for score, _, _ in scored)
        if longest < MIN_USEFUL_CHARS:
            return None
        # Anything within a tenth of the best is just as good a container, so
        # the smallest of them is the one that excludes the surrounding chrome.
        near_best = [row for row in scored if row[0] >= longest * 0.9]
        return min(near_best, key=lambda row: row[1])[2]

    # ------------------------------------------------------------------ #
    @staticmethod
    def _cutoff(max_seconds):
        """Normalises the watch position, treating 0/absent as "the whole thing"."""
        if max_seconds is None:
            return None
        try:
            value = float(max_seconds)
        except (TypeError, ValueError):
            return None
        return value if value > 0 else None

    @staticmethod
    def _empty(message: str) -> dict:
        return {
            "available": False,
            "source": "",
            "text": "",
            "duration_seconds": None,
            "covered_seconds": None,
            "truncated": False,
            "message": message,
        }
