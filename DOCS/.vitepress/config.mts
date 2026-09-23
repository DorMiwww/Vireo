import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'Vireo',
  description: 'Design as a Code (DaaC) platform & compiler',
  base: '/Vireo/',
  cleanUrls: true,

  markdown: {
    languageAlias: {
      dac: 'kotlin'
    }
  },

  themeConfig: {
    siteTitle: 'Vireo DaaC',

    nav: [
      { text: 'Home', link: '/' },
      { text: 'Getting Started', link: '/getting-started' },
      { text: 'Components', link: '/components/' },
      { text: 'CLI', link: '/cli' },
      { text: 'Figma', link: '/figma-plugin' }
    ],

    sidebar: [
      {
        text: 'Introduction',
        items: [
          { text: 'Overview', link: '/' },
          { text: 'Getting Started', link: '/getting-started' }
        ]
      },
      {
        text: 'Core Concepts',
        items: [
          { text: 'Fundamentals', link: '/concepts/' },
          { text: 'Addressing & Composition', link: '/concepts/addressing' },
          { text: 'Expressions & Variables', link: '/concepts/expressions' }
        ]
      },
      {
        text: 'Component Reference',
        items: [
          { text: 'Component Model', link: '/components/' },
          { text: 'Properties Catalogue', link: '/components/properties' },
          { text: 'Buttons & Actions', link: '/components/buttons' },
          { text: 'Badges & Indicators', link: '/components/badges' },
          { text: 'Containers & Cards', link: '/components/containers' },
          { text: 'Forms & Inputs', link: '/components/forms' }
        ]
      },
      {
        text: 'Layout System',
        items: [
          { text: 'Auto Layout & Constraints', link: '/layout' }
        ]
      },
      {
        text: 'Renderers & Targets',
        items: [
          { text: 'Renderers Overview', link: '/renderers/' },
          { text: 'HTML Renderer', link: '/renderers/html' },
          { text: 'JSON AST', link: '/renderers/json' },
          { text: 'Figma Workflow', link: '/renderers/figma' }
        ]
      },
      {
        text: 'CLI & Tooling',
        items: [
          { text: 'CLI Reference', link: '/cli' },
          { text: 'Figma Plugin Guide', link: '/figma-plugin' }
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/DorMiwww/Vireo' }
    ],

    search: {
      provider: 'local'
    },

    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Copyright © 2026 Vireo Contributors'
    }
  }
})
