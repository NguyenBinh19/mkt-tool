import { useState } from 'react';
import { facebookApi } from '../lib/apiClient';
import { validateFacebookPostUrl } from '../lib/validation';

const INITIAL_FORM = { postUrl: '', maxComments: 100, includeReplies: true };

export function useFacebookExtraction() {
  const [form, setForm] = useState(INITIAL_FORM);
  const [result, setResult] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [notice, setNotice] = useState(null);

  const updateField = (field, value) => setForm((current) => ({ ...current, [field]: value }));

  async function openFacebookSession() {
    setIsLoading(true);
    setNotice(null);
    try {
      const message = await facebookApi.openBrowser();
      setNotice({ type: 'success', text: message });
    } catch (error) {
      setNotice({ type: 'error', text: error.message });
    } finally {
      setIsLoading(false);
    }
  }

  async function extractComments() {
    const validationError = validateFacebookPostUrl(form.postUrl);
    if (validationError) {
      setNotice({ type: 'error', text: validationError });
      return null;
    }
    setIsLoading(true);
    setNotice(null);
    try {
      const data = await facebookApi.extractComments({ ...form, maxComments: Number(form.maxComments) });
      setResult(data);
      setNotice({ type: data.status === 'ok' ? 'success' : 'error', text: data.message });
      return data;
    } catch (error) {
      setNotice({ type: 'error', text: error.message });
      return null;
    } finally {
      setIsLoading(false);
    }
  }

  return { form, result, isLoading, notice, updateField, openFacebookSession, extractComments };
}
