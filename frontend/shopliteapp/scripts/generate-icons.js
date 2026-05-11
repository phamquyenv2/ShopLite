#!/usr/bin/env node

/**
 * Generate app icons and splash screens from a source image.
 *
 * Usage:
 *   node scripts/generate-icons.js <path-to-source-image.png>
 *
 * Requirements:
 *   - Source image should be at least 1024x1024 PNG
 *   - Install: npm install -g sharp-cli (or use the npm script below)
 *
 * This script generates:
 *   - Android mipmap icons (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
 *   - Android splash screens (portrait + landscape)
 */

import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const sourceImage = process.argv[2];
if (!sourceImage) {
  console.error('Usage: node generate-icons.js <path-to-source-image.png>');
  console.error('Source image should be 1024x1024 PNG');
  process.exit(1);
}

if (!fs.existsSync(sourceImage)) {
  console.error(`File not found: ${sourceImage}`);
  process.exit(1);
}

const androidResDir = path.join(__dirname, '..', 'android', 'app', 'src', 'main', 'res');

const iconSizes = {
  'mipmap-mdpi': { size: 48, round: false },
  'mipmap-hdpi': { size: 72, round: false },
  'mipmap-xhdpi': { size: 96, round: false },
  'mipmap-xxhdpi': { size: 144, round: false },
  'mipmap-xxxhdpi': { size: 192, round: false },
};

const splashSizes = [
  { dir: 'drawable-mdpi', width: 320, height: 480 },
  { dir: 'drawable-hdpi', width: 480, height: 800 },
  { dir: 'drawable-xhdpi', width: 720, height: 1280 },
  { dir: 'drawable-xxhdpi', width: 960, height: 1600 },
  { dir: 'drawable-xxxhdpi', width: 1280, height: 1920 },
  { dir: 'drawable-land-mdpi', width: 480, height: 320 },
  { dir: 'drawable-land-hdpi', width: 800, height: 480 },
  { dir: 'drawable-land-xhdpi', width: 1280, height: 720 },
  { dir: 'drawable-land-xxhdpi', width: 1600, height: 960 },
  { dir: 'drawable-land-xxxhdpi', width: 1920, height: 1280 },
];

function run(cmd) {
  try {
    execSync(cmd, { stdio: 'pipe' });
    return true;
  } catch {
    return false;
  }
}

// Check if sharp is available
const hasSharp = run('npx sharp-cli --version');
if (!hasSharp) {
  console.log('Installing sharp-cli...');
  run('npm install -g sharp-cli');
}

console.log('Generating Android icons...');

for (const [dir, config] of Object.entries(iconSizes)) {
  const dirPath = path.join(androidResDir, dir);
  fs.mkdirSync(dirPath, { recursive: true });

  // Standard icon
  const iconPath = path.join(dirPath, 'ic_launcher.png');
  run(`npx sharp-cli -i "${sourceImage}" -o "${iconPath}" resize ${config.size} ${config.size}`);
  console.log(`  Generated: ${dir}/ic_launcher.png (${config.size}x${config.size})`);

  // Round icon
  const roundPath = path.join(dirPath, 'ic_launcher_round.png');
  run(`npx sharp-cli -i "${sourceImage}" -o "${roundPath}" resize ${config.size} ${config.size}`);
  console.log(`  Generated: ${dir}/ic_launcher_round.png (${config.size}x${config.size})`);

  // Foreground icon (adaptive)
  const fgPath = path.join(dirPath, 'ic_launcher_foreground.png');
  const fgSize = Math.round(config.size * 2.67); // 108dp at xxxhdpi
  run(`npx sharp-cli -i "${sourceImage}" -o "${fgPath}" resize ${fgSize} ${fgSize}`);
  console.log(`  Generated: ${dir}/ic_launcher_foreground.png (${fgSize}x${fgSize})`);
}

console.log('\nGenerating splash screens...');

for (const splash of splashSizes) {
  const dirPath = path.join(androidResDir, splash.dir);
  fs.mkdirSync(dirPath, { recursive: true });

  const splashPath = path.join(dirPath, 'splash.png');
  // Center-crop the source image to splash dimensions
  run(`npx sharp-cli -i "${sourceImage}" -o "${splashPath}" resize ${splash.width} ${splash.height} --fit cover`);
  console.log(`  Generated: ${splash.dir}/splash.png (${splash.width}x${splash.height})`);
}

console.log('\nDone! Icons and splash screens generated.');
console.log('You may also want to update drawable/ic_launcher_background.xml with your brand color.');
