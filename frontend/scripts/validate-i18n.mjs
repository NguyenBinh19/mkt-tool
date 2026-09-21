import { readFile, readdir } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { translations } from '../src/i18n/translations.js';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../src');

async function sourceFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const nested = await Promise.all(entries.map((entry) => {
    const target = path.join(directory, entry.name);
    if (entry.isDirectory()) return sourceFiles(target);
    return /\.(js|jsx)$/.test(entry.name) ? [target] : [];
  }));
  return nested.flat();
}

const files = await sourceFiles(root);
const usedKeys = new Set();
for (const file of files) {
  const source = await readFile(file, 'utf8');
  for (const match of source.matchAll(/\bt\(['"]([^'"]+)['"]/g)) usedKeys.add(match[1]);
}

const languages = Object.keys(translations);
const missing = languages.flatMap((language) => [...usedKeys]
  .filter((key) => !key.includes('${') && translations[language]?.[key] === undefined)
  .map((key) => `${language}: ${key}`));

if (missing.length) {
  console.error(`Missing translations:\n${missing.join('\n')}`);
  process.exitCode = 1;
} else {
  console.log(`i18n OK: ${usedKeys.size} literal keys across ${languages.length} languages.`);
}
