/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
        "./src/**/*.{html,ts}",
    ],
    theme: {
        extend: {
            colors: {
                'cs2-dark': '#0f172a',
                'cs2-darker': '#020617',
                'cs2-orange': '#f97316',
                'cs2-orange-hover': '#ea580c',
                'cs2-blue': '#3b82f6',
                'cs2-text': '#f8fafc',
                'cs2-muted': '#94a3b8',
            }
        },
    },
    plugins: [],
}
