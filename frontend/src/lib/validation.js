export function validateFacebookPostUrl(value, t = (key) => key) {
  if (!value?.trim()) return t('validation.facebookRequired');
  try {
    const url = new URL(value.trim());
    const host = url.hostname.toLowerCase();
    const isFacebookHost = host === 'facebook.com' || host.endsWith('.facebook.com') || host === 'fb.watch';
    if (url.protocol !== 'https:' || !isFacebookHost) return t('validation.facebookInvalid');
    return null;
  } catch {
    return t('validation.facebookMalformed');
  }
}
