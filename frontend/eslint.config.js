import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{js,jsx}'],
    extends: [
      js.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
      parserOptions: {
        ecmaVersion: 'latest',
        ecmaFeatures: { jsx: true },
        sourceType: 'module',
      },
    },
    rules: {
      'no-unused-vars': ['error', { varsIgnorePattern: '^[A-Z_]' }],
      'react-refresh/only-export-components': 'off',
      // Quy tắc mới của React Compiler (eslint-plugin-react-hooks 7.1): gọi setState đồng bộ trong useEffect /
      // memo thủ công không giữ được. Là khuyến nghị hiệu năng, không phải lỗi chức năng — để mức cảnh báo
      // và xử lý khi tách/refactor các trang lớn ở P3-11.
      'react-hooks/set-state-in-effect': 'warn',
      'react-hooks/preserve-manual-memoization': 'warn',
    },
  },
  {
    // File cấu hình chạy trong Node (dùng process.env...), không phải trong trình duyệt
    files: ['*.config.js'],
    languageOptions: { globals: globals.node },
  },
])
