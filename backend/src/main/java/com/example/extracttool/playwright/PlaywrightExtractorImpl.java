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

            selectAllComments(page);

            List<CommentDTO> result = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            int unchangedRounds = 0;
            for (int round = 0; round < 150 && result.size() < maxComments && unchangedRounds < 12; round++) {
                expandVisibleCommentControls(page, includeReplies);
                page.waitForTimeout(900);
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
                page.waitForTimeout(900);
            }
            return result;
        }
    }

    private void selectAllComments(Page page) {
        // Facebook has several labels for this control, depending on locale and account UI.
        // Use a real Playwright click so the menu receives the full pointer event sequence.
        String[] sortLabels = {"Phù hợp nhất", "Most relevant", "Relevant comments", "Mới nhất", "Newest"};
        for (String label : sortLabels) {
            if (clickVisibleExactText(page, label)) {
                page.waitForTimeout(650);
                break;
            }
        }
        String[] allLabels = {"Tất cả bình luận", "All comments", "Tất cả các bình luận"};
        for (String label : allLabels) {
            if (clickVisibleExactText(page, label)) {
                page.waitForTimeout(1_200);
                break;
            }
        }
    }

    private boolean clickVisibleExactText(Page page, String text) {
        // Native Playwright clicking sends the complete pointer sequence Facebook expects for its menus.
        // A DOM element.click() call is insufficient for several Facebook UI variants.
        Locator options = page.getByText(text);
        for (int index = options.count() - 1; index >= 0; index--) {
            Locator option = options.nth(index);
            if (option.isVisible()) {
                option.click(new Locator.ClickOptions().setForce(true));
                return true;
            }
        }
        return false;
    }

    private void expandVisibleCommentControls(Page page, boolean includeReplies) {
        page.locator("button, [role='button']").evaluateAll("""
                (buttons, includeReplies) => {
                  const pattern = includeReplies
                    ? /(view|see|more|xem|thêm).*(comment|reply|bình luận|phản hồi|câu trả lời)|\\d+\\s+(more|bình luận|phản hồi|câu trả lời)/i
                    : /(view|see|more|xem|thêm).*(comment|bình luận)|\\d+\\s+(more|bình luận)/i;
                  buttons.forEach(button => {
                    const text = (button.innerText || button.getAttribute('aria-label') || '').trim();
                    const rect = button.getBoundingClientRect();
                    const genericMore = /^(view more|see more|xem thêm)$/i.test(text);
                    if ((pattern.test(text) || genericMore) && rect.width > 0 && rect.height > 0) button.click();
                  });
                }
                """, includeReplies);
        // These controls can be rendered as links instead of buttons on group posts.
        String[] loadLabels = {"Xem thêm bình luận", "Xem các bình luận khác", "View more comments", "See more comments", "View previous comments"};
        for (String label : loadLabels) {
            clickVisibleText(page, label, 5);
        }
    }

    private void scrollCommentDialog(Page page) {
        page.locator("[role='dialog']").evaluateAll("""
                dialogs => {
                  const dialog = dialogs[dialogs.length - 1];
                  if (!dialog) {
                    window.scrollBy(0, Math.max(window.innerHeight * 0.9, 850));
                    return;
                  }
                  const scrollable = [dialog, ...dialog.querySelectorAll('div')]
                    .filter(element => element.scrollHeight > element.clientHeight + 24)
                    .sort((left, right) => right.scrollHeight - left.scrollHeight)[0];
                  if (scrollable) {
                    scrollable.scrollTop += Math.max(scrollable.clientHeight * 0.85, 650);
                  } else {
                    dialog.scrollTop += Math.max(dialog.clientHeight * 0.85, 650);
                  }
                }
                """);
    }

    private int clickVisibleText(Page page, String text, int limit) {
        int clicked = 0;
        Locator matches = page.getByText(text);
        for (int index = matches.count() - 1; index >= 0 && clicked < limit; index--) {
            Locator match = matches.nth(index);
            if (match.isVisible()) {
                match.click(new Locator.ClickOptions().setForce(true));
                clicked++;
                page.waitForTimeout(250);
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
                    const message = contentFromAncestors(anchor, author);
                    const timestamp = node.querySelector('abbr')?.getAttribute('title') || node.querySelector('time')?.getAttribute('datetime') || null;
                    return { id, parentId, author, message, createdAt: timestamp };
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
        return CommentDTO.builder().id(id).parentId(value(item, "parentId")).author(author).message(message)
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
