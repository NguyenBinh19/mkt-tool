export function validateFacebookPostUrl(value) {
  if (!value?.trim()) return 'Vui lòng nhập đường dẫn bài viết Facebook.';
  try {
    const url = new URL(value.trim());
    const host = url.hostname.toLowerCase();
    const isFacebookHost = host === 'facebook.com' || host.endsWith('.facebook.com') || host === 'fb.watch';
    if (url.protocol !== 'https:' || !isFacebookHost) return 'Đường dẫn phải là URL Facebook hợp lệ (https://...).';
    return null;
  } catch {
    return 'Đường dẫn chưa đúng định dạng. Hãy dán link bài viết Facebook đầy đủ.';
  }
}
