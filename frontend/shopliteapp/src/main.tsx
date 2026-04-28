import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';

// Ensure Ionic mode class is set consistently (controls typography variables).
document.documentElement.classList.add('md');
document.documentElement.classList.remove('ios');

const container = document.getElementById('root');
const root = createRoot(container!);
root.render(
  // <React.StrictMode>
  <App />
  // </React.StrictMode>
);