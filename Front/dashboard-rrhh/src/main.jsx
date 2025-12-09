import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';

import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap-icons/font/bootstrap-icons.css';
import './styles/base.css';
import './styles/components.css';
import './styles/navbar.css';
import './styles/sidebar.css';

createRoot(document.getElementById('root')).render(<App />);