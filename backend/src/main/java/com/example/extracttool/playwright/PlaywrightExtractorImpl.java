package com.example.extracttool.playwright;

import com.example.extracttool.config.BrowserSession;
import com.example.extracttool.model.dto.CommentDTO;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.example.extracttool.model.dto.FacebookPostDTO;

@Component
public class PlaywrightExtractorImpl implements PlaywrightExtractor {
    private static final String COMMENT_LINKS = "a[href*='comment_id='], a[href*='/comment/'], [data-commentid], [data-comment-id]";
    private final BrowserSession browserSession;

    public PlaywrightExtractorImpl(BrowserSession browserSession) {
        this.browserSession = browserSession;
    }

    @Override
    public List<CommentDTO> extract(String postUrl, int maxComments, boolean includeReplies) {
        BrowserContext context = browserSession.context();
        try (Page page = context.newPage()) {
            page.navigate(postUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForTimeout(2_500);
            if (page.url().contains("/login") || page.locator("input[name='email']").count() > 0) {
                throw new IllegalStateException("Facebook is not signed in. Open /api/facebook/browser first, sign in in the displayed browser, then retry.");
            }

            dismissContentIntroPopup(page);
            selectAllComments(page);

            List<CommentDTO> result = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            int unchangedRounds = 0;
            for (int round = 0; round < 150 && result.size() < maxComments && unchangedRounds < 12; round++) {
                dismissContentIntroPopup(page);
                expandVisibleCommentControls(page, includeReplies);
                page.waitForTimeout(650);
                int before = result.size();
                for (Map<String, Object> item : readVisibleComments(page)) {
                    CommentDTO comment = toComment(item);
                    if (comment == null || (!includeReplies && comment.getParentId() != null) || !seen.add(comment.getId())) {
                        continue;
                    }
                    result.add(comment);
                    if (result.size() == maxComments) break;
                }
                unchangedRounds = result.size() == before ? unchangedRounds + 1 : 0;
                scrollCommentDialog(page);
                page.waitForTimeout(650);
            }
            return result;
        }
    }

    @Override
    public List<FacebookPostDTO> searchPosts(String keyword, int limit, boolean recentPosts) {
        BrowserContext context = browserSession.context();
        try (Page page = context.newPage()) {
            page.navigate("https://www.facebook.com/", new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForTimeout(1_500);
            Locator search = page.locator("input[aria-label*='Tìm kiếm'], input[aria-label*='Search'], input[placeholder*='Tìm kiếm'], input[placeholder*='Search']").first();
            search.waitFor(new Locator.WaitForOptions().setTimeout(10_000));
            search.fill(keyword);
            search.press("Enter");
            page.waitForTimeout(1_800);

            if (recentPosts) {
                selectRecentPostResults(page);
                page.waitForTimeout(1_500);
            }

            Set<String> seen = new HashSet<>();
            Set<String> visitedCards = new HashSet<>();
            List<FacebookPostDTO> results = new ArrayList<>();
            for (int round = 0; round < 10 && results.size() < limit; round++) {
                Object raw = page.locator("[role='feed'] a[href], [role='main'] a[href]").evaluateAll("""
                        (anchors, searchKeyword) => {
                          const isPostUrl = href => /\\/posts\\/|\\/permalink\\.php|story_fbid=|multi_permalinks=|pfbid|fbid=/i.test(href || '') && !/\\/(reel|videos?)\\//i.test(href || '');
                          const compact = value => (value || '').replace(/\\s+/g, ' ').trim();
                          const keyword = compact(searchKeyword).toLocaleLowerCase();
                          const contentOf = (container, query) => {
                            const candidates = [...container.querySelectorAll('[data-ad-preview="message"], [dir=auto]')]
                              .map(item => compact(item.innerText))
                              .filter(text => text.length > 20 && text.length < 4000);
                            const matching = candidates.filter(text => !query || text.toLocaleLowerCase().includes(query));
                            return (matching.length ? matching : candidates).sort((left, right) => right.length - left.length)[0] || compact(container.innerText).slice(0, 4000);
                          };
                          const findContainer = anchor => {
                            let node = anchor;
                            for (let depth = 0; depth < 9 && node; depth++, node = node.parentElement) {
                              if (node.matches?.('[role=article], [data-pagelet*="FeedUnit"]')) return node;
                              const text = compact(node.innerText);
                              if (text.length > 120 && text.length < 6000) return node;
                            }
                            return anchor.parentElement || anchor;
                          };
                          return anchors.map(anchor => {
                            if (!isPostUrl(anchor.href)) return null;
                            const container = findContainer(anchor);
                            const text = contentOf(container || anchor, keyword);
                            const rect = anchor.getBoundingClientRect();
                            return { url: anchor.href, text, visible: rect.width > 0 && rect.height > 0 };
                          }).filter(item => item && item.url && item.visible && (!keyword || item.text.toLocaleLowerCase().includes(keyword)));
                        }
                        """, keyword);
                if (raw instanceof List<?> values) {
                    for (Object value : values) {
                        if (!(value instanceof Map<?, ?> item)) continue;
                        String url = item.get("url") == null ? null : item.get("url").toString().trim();
                        if (url == null || !seen.add(url)) continue;
                        String text = item.get("text") == null ? null : item.get("text").toString().trim();
                        results.add(FacebookPostDTO.builder().url(url).text(text).build());
                        if (results.size() >= limit) break;
                    }
                }
                if (results.size() < limit && round < 5) {
                    recoverPostsFromCommentDialogs(page, keyword, limit, seen, visitedCards, results);
                }
                if (results.size() < limit) {
                    int beforeLinks = page.locator("[role='feed'] a[href], [role='main'] a[href]").count();
                    page.mouse().wheel(0, 850);
                    waitForFeedToSettle(page, beforeLinks);
                }
            }
            return results;
        }
    }

    private void recoverPostsFromCommentDialogs(Page page, String keyword, int limit, Set<String> seen, Set<String> visitedCards, List<FacebookPostDTO> results) {
        Locator feed = page.locator("[role='feed']");
        Locator cards = feed.count() > 0 ? feed.locator("[role='article'], div[data-pagelet*='FeedUnit']") : page.locator("[role='main'] [role='article'], [role='main'] div[data-pagelet*='FeedUnit']");
        for (int index = 0; index < cards.count() && results.size() < limit; index++) {
            Locator card = cards.nth(index);
            if (!card.isVisible()) continue;
            String cardText = card.innerText();
            if (cardText == null || cardText.isBlank()) continue;
            String fingerprint = fingerprint(cardText);
            if (!visitedCards.add(fingerprint)) continue;

            String postUrl = findPostUrlIn(card);
            if (postUrl == null) {
                Locator controls = card.locator("button, [role='button'], a");
                for (int controlIndex = 0; controlIndex < controls.count(); controlIndex++) {
                    Locator control = controls.nth(controlIndex);
                    if (!control.isVisible()) continue;
                    String label = ((control.getAttribute("aria-label") == null ? "" : control.getAttribute("aria-label")) + " " + (control.innerText() == null ? "" : control.innerText())).toLowerCase();
                    if (!label.contains("bình luận") && !label.contains("comment")) continue;
                    try {
                        control.click();
                        page.waitForTimeout(1_800);
                        postUrl = findPostUrlIn(page.locator("[role='dialog'] a[href]"));
                        if (postUrl == null) postUrl = findPostUrlIn(page.locator("a[href]"));
                        page.keyboard().press("Escape");
                        page.waitForTimeout(650);
                    } catch (RuntimeException ignored) {
                        // A virtualized card may disappear while Facebook opens the dialog.
                    }
                    if (postUrl != null) break;
                }
            }
            if (postUrl != null && seen.add(postUrl)) {
                results.add(FacebookPostDTO.builder().url(postUrl).text(cardText.trim()).build());
            }
        }

        // Newer Facebook search layouts do not mark feed cards with role=article.
        // In that case inspect visible comment controls directly and recover the
        // post URL from the dialog opened by the real click.
        if (results.size() >= limit) return;
        Locator controls = feed.count() > 0 ? feed.locator("button, [role='button'], a") : page.locator("[role='main'] button, [role='main'] [role='button'], [role='main'] a");
        for (int index = 0; index < controls.count() && results.size() < limit; index++) {
            Locator control = controls.nth(index);
            if (!control.isVisible()) continue;
            String aria = control.getAttribute("aria-label");
            String visibleText = control.innerText();
            String label = ((aria == null ? "" : aria) + " " + (visibleText == null ? "" : visibleText)).trim();
            if (!label.matches("(?i).*\\b(comment|bình luận)\\b.*") || label.matches("(?i).*(like|thích).*comment.*")) continue;

            String cardText = findKeywordContext(control);
            if (cardText == null || cardText.isBlank()) continue;
            String fingerprint = fingerprint(cardText);
            if (!visitedCards.add(fingerprint)) continue;
            try {
                control.click();
                page.waitForTimeout(1_800);
                String postUrl = findPostUrlIn(page.locator("[role='dialog'] a[href]"));
                page.keyboard().press("Escape");
                page.waitForTimeout(650);
                if (postUrl != null && seen.add(postUrl)) {
                    results.add(FacebookPostDTO.builder().url(postUrl).text(cardText.trim()).build());
                }
            } catch (RuntimeException ignored) {
                try { page.keyboard().press("Escape"); } catch (RuntimeException ignoredAgain) { }
            }
        }
    }

    private String fingerprint(String text) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.substring(0, Math.min(normalized.length(), 320));
    }

    private void waitForFeedToSettle(Page page, int beforeLinks) {
        int stableRounds = 0;
        int previousLinks = beforeLinks;
        for (int attempt = 0; attempt < 8; attempt++) {
            page.waitForTimeout(1_000);
            int currentLinks = page.locator("[role='feed'] a[href], [role='main'] a[href]").count();
            if (currentLinks == previousLinks) stableRounds++; else stableRounds = 0;
            previousLinks = currentLinks;
            if (stableRounds >= 3) return;
        }
    }

    private String findKeywordContext(Locator control) {
        Locator parent = control;
        for (int level = 0; level < 9; level++) {
            parent = parent.locator("xpath=..");
            if (!parent.isVisible()) continue;
            String text = parent.innerText();
            if (text != null && text.length() > 50 && text.length() < 8_000) return text;
        }
        return null;
    }

    private String findPostUrlIn(Locator links) {
        for (int index = 0; index < links.count(); index++) {
            String href = links.nth(index).getAttribute("href");
            if (isPostHref(href)) return href.startsWith("/") ? "https://www.facebook.com" + href : href;
        }
        return null;
    }

    private boolean isPostHref(String href) {
        return href != null && href.matches("(?i).*(/posts/|/permalink\\.php|story_fbid=|multi_permalinks=|pfbid|fbid=).*")
                && !href.matches("(?i).*/(reel|videos?)/.*");
    }

    private void selectRecentPostResults(Page page) {
        // Search result controls vary by locale and account. Every interaction
        // below is a real Playwright click; no DOM click is injected.
        String[] postTabs = {"Bài viết", "Posts"};
        for (String label : postTabs) {
            if (clickVisibleExactText(page, label)) {
                page.waitForTimeout(700);
                break;
            }
        }

        // In the current Vietnamese UI this is a switch in the left filter
        // rail, not a menu item. Click the actual switch next to its label.
        String[] recentSwitches = {"Bài viết mới đây", "Recent posts"};
        for (String label : recentSwitches) {
            if (clickLabeledControl(page, label)) {
                page.waitForTimeout(900);
                break;
            }
        }

        String[] filterLabels = {"Bộ lọc", "Filters", "Bộ lọc tìm kiếm", "Search filters"};
        for (String label : filterLabels) {
            if (clickVisibleExactText(page, label)) {
                page.waitForTimeout(450);
                break;
            }
        }

        // Do not click the switch label again here: that would toggle it back off.
        String[] recentLabels = {"Bài viết gần đây", "Most recent", "Mới nhất"};
        for (String label : recentLabels) {
            if (clickVisibleExactText(page, label)) {
                page.waitForTimeout(450);
                break;
            }
        }

        String[] applyLabels = {"Hiển thị kết quả", "Xem kết quả", "Show results", "Apply"};
        for (String label : applyLabels) {
            if (clickVisibleExactText(page, label) || clickVisibleText(page, label, 1) > 0) {
                page.waitForTimeout(1_200);
                break;
            }
        }
    }

    private boolean clickLabeledControl(Page page, String label) {
        Locator labels = page.getByText(label, new Page.GetByTextOptions().setExact(true));
        for (int index = labels.count() - 1; index >= 0; index--) {
            Locator text = labels.nth(index);
            if (!text.isVisible()) continue;
            for (int level = 0; level < 4; level++) {
                Locator parent = text;
                for (int step = 0; step <= level; step++) parent = parent.locator("xpath=..");
                Locator control = parent.locator("button, [role='switch'], [role='checkbox'], input[type='checkbox']");
                if (control.count() > 0 && control.first().isVisible()) {
                    try {
                        control.first().click();
                        return true;
                    } catch (RuntimeException ignored) {
                        // Try the next containing level if Facebook re-rendered it.
                    }
                }
            }
        }
        return false;
    }

    /**
     * Facebook sometimes displays an informational modal titled "Giới thiệu về
     * nội dung này" ("About this content") before the comment controls become
     * interactive. It is not part of the post and can intercept every click.
     */
    private void dismissContentIntroPopup(Page page) {
        // Only inspect dialogs that actually contain this informational popup;
        // never click generic "Close" or "OK" buttons elsewhere on the page.
        Locator dialogs = page.locator("[role='dialog']");
        for (int index = dialogs.count() - 1; index >= 0; index--) {
            Locator dialog = dialogs.nth(index);
            if (!dialog.isVisible()) continue;
            String text = dialog.innerText();
            if (text != null && (text.contains("Giới thiệu về nội dung này") || text.contains("About this content"))) {
                Locator close = dialog.locator("button[aria-label='Đóng'], button[aria-label='Close'], [role='button'][aria-label='Đóng'], [role='button'][aria-label='Close']");
                if (close.count() > 0 && close.first().isVisible()) {
                    close.first().click();
                } else {
                    dialog.press("Escape");
                }
            }
        }
        page.waitForTimeout(200);
    }

    private void selectAllComments(Page page) {
        // Wait for Facebook's comment surface before looking for its sort menu.
        // On some post formats the menu is rendered a little after DOMContentLoaded.
        try {
            page.locator(COMMENT_LINKS).first().waitFor(new Locator.WaitForOptions().setTimeout(10_000));
        } catch (RuntimeException ignored) {
            // Continue; Facebook can expose the sort control before comment links.
        }

        // Facebook has several labels for this control, depending on locale and account UI.
        // Use a real Playwright click so the menu receives the full pointer event sequence.
        String[] sortLabels = {"Phù hợp nhất", "Most relevant", "Relevant comments", "Mới nhất", "Newest", "Top comments", "Most recent"};
        for (String label : sortLabels) {
            if (clickVisibleExactText(page, label) || clickVisibleText(page, label, 1) > 0) {
                page.waitForTimeout(500);
                break;
            }
        }
        String[] allLabels = {"Tất cả bình luận", "All comments", "Tất cả các bình luận", "All comments and replies", "All"};
        for (String label : allLabels) {
            if (clickVisibleMenuItem(page, label) || clickVisibleExactText(page, label) || clickVisibleText(page, label, 1) > 0) {
                page.waitForTimeout(900);
                break;
            }
        }
    }

    private boolean clickVisibleMenuItem(Page page, String text) {
        Locator items = page.locator("[role='menuitem'], [role='option'], [role='menu'] button");
        for (int index = items.count() - 1; index >= 0; index--) {
            Locator item = items.nth(index);
            if (!item.isVisible()) continue;
            String label = item.innerText();
            if (label != null && label.trim().equalsIgnoreCase(text)) {
                try {
                    item.click();
                    return true;
                } catch (RuntimeException ignored) {
                    // Facebook may recycle the menu item after opening the menu.
                }
            }
        }
        return false;
    }

    private boolean clickVisibleExactText(Page page, String text) {
        // Native Playwright clicking sends the complete pointer sequence Facebook expects for its menus.
        // A DOM element.click() call is insufficient for several Facebook UI variants.
        Locator options = page.getByText(text, new Page.GetByTextOptions().setExact(true));
        for (int index = options.count() - 1; index >= 0; index--) {
            Locator option = options.nth(index);
            if (option.isVisible()) {
                try {
                    option.click();
                    return true;
                } catch (RuntimeException ignored) {
                    // A stale virtualized menu item can disappear between the
                    // visibility check and click; try the next matching item.
                }
            }
        }
        return false;
    }

    private void expandVisibleCommentControls(Page page, boolean includeReplies) {
        // Use real Playwright clicks on known labels instead of clicking every
        // matching DOM button. This prevents unrelated Facebook controls from
        // opening overlays and makes the flow deterministic.
        String[] loadLabels = includeReplies
                ? new String[]{"Xem thêm bình luận", "Xem các bình luận khác", "Xem thêm phản hồi", "Xem câu trả lời", "View more comments", "See more comments", "View previous comments", "View more replies", "See replies"}
                : new String[]{"Xem thêm bình luận", "Xem các bình luận khác", "View more comments", "See more comments", "View previous comments"};
        for (String label : loadLabels) {
            clickVisibleText(page, label, 5);
        }
    }

    private void scrollCommentDialog(Page page) {
        Locator dialogs = page.locator("[role='dialog']");
        if (dialogs.count() > 0 && dialogs.last().isVisible()) {
            // Scroll with a real mouse wheel over the dialog, not by mutating
            // scrollTop from JavaScript. Facebook then receives the same event
            // sequence as a user scrolling its comment panel.
            dialogs.last().hover();
            page.mouse().wheel(0, 700);
        } else {
            page.mouse().wheel(0, 850);
        }
    }

    private int clickVisibleText(Page page, String text, int limit) {
        int clicked = 0;
        Locator matches = page.getByText(text);
        for (int index = matches.count() - 1; index >= 0 && clicked < limit; index--) {
            Locator match = matches.nth(index);
            if (match.isVisible()) {
                try {
                    match.click();
                    clicked++;
                    page.waitForTimeout(250);
                } catch (RuntimeException ignored) {
                    // Facebook can re-render this menu immediately after a click.
                }
            }
        }
        return clicked;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readVisibleComments(Page page) {
        Object raw = page.locator(COMMENT_LINKS).evaluateAll("""
                anchors => {
                  const compact = value => (value || '').replace(/\\s+/g, ' ').trim();
                  const query = href => href ? new URL(href, location.href).searchParams : new URLSearchParams();
                  const hrefOf = element => element.href || element.closest('a')?.href || '';
                  const closestComment = anchor => {
                    let node = anchor.tagName === 'A' ? anchor.parentElement : anchor;
                    for (let i = 0; i < 8 && node; i++, node = node.parentElement) {
                      const text = compact(node.innerText);
                      const links = node.querySelectorAll('a[href]').length;
                      const commentLinks = node.querySelectorAll("a[href*='comment_id=']").length;
                      if (text.length >= 2 && text.length <= 5000 && links >= 2 && commentLinks <= 2) return node;
                    }
                    return anchor.tagName === 'A' ? anchor.parentElement : anchor;
                  };
                  const isMetadata = text => /^(\\d+\\s*(m|h|d|w|y|min|phút|giờ|ngày|tuần|tháng|năm)|thích|phản hồi|trả lời|của tác giả|author)$/i.test(text);
                  const contentFromAncestors = (anchor, author) => {
                    let ancestor = anchor.parentElement;
                    for (let depth = 0; depth < 10 && ancestor; depth++, ancestor = ancestor.parentElement) {
                      const candidates = [...new Set([...ancestor.querySelectorAll('[dir=auto]')]
                        .map(element => compact(element.innerText))
                        .filter(text => text && text !== author && !isMetadata(text) && text.length < 4000))];
                      if (candidates.length > 0) return candidates.sort((left, right) => left.length - right.length)[0];
                    }
                    return '';
                  };
                  return anchors.map(anchor => {
                    const href = hrefOf(anchor);
                    const params = query(href);
                    const pathId = href.match(/\\/comment\\/(\\d+)/)?.[1];
                    const dataId = anchor.getAttribute('data-commentid') || anchor.getAttribute('data-comment-id');
                    const id = params.get('reply_comment_id') || params.get('comment_id') || pathId || dataId;
                    const parentId = params.get('reply_comment_id') ? params.get('comment_id') : anchor.getAttribute('data-parent-comment-id');
                    const node = closestComment(anchor);
                    const authorLink = [...node.querySelectorAll('a[role=link], a[href]')]
                      .find(link => link !== anchor && compact(link.innerText).length > 0);
                    const author = compact(authorLink?.innerText);
                    const authorUrl = authorLink?.href || null;
                    const message = contentFromAncestors(anchor, author);
                    const timestamp = node.querySelector('abbr')?.getAttribute('title') || node.querySelector('time')?.getAttribute('datetime') || null;
                    return { id, parentId, author, authorUrl, message, createdAt: timestamp };
                  }).filter(comment => /^\\d+$/.test(comment.id || '') && comment.author);
                }
                """);
        if (!(raw instanceof List<?> values)) return List.of();
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object value : values) if (value instanceof Map<?, ?> map) items.add((Map<String, Object>) map);
        return items;
    }

    private CommentDTO toComment(Map<String, Object> item) {
        String id = value(item, "id");
        String author = value(item, "author");
        String message = value(item, "message");
        if (id == null || author == null) return null;
        return CommentDTO.builder().id(id).parentId(value(item, "parentId")).author(author).authorUrl(value(item, "authorUrl")).message(message)
                .createdAt(parseInstant(value(item, "createdAt"))).build();
    }

    private String value(Map<String, Object> item, String key) {
        Object value = item.get(key);
        return value == null || value.toString().isBlank() ? null : value.toString().trim();
    }

    private Instant parseInstant(String value) {
        if (value == null) return null;
        try { return Instant.parse(value); } catch (DateTimeParseException ignored) { return null; }
    }
}
