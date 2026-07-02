/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        // Masri brand red (sampled from the logo wordmark ~ #C8102E)
        brand: {
          50:  '#fef2f3',
          100: '#fde2e4',
          200: '#fbccd0',
          300: '#f7a3aa',
          400: '#f06b76',
          500: '#e23744',
          600: '#c8102e',
          700: '#a60d26',
          800: '#891025',
          900: '#741224',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
