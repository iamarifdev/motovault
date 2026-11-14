# MotoVault app UI v4: UX & Motion evolution prototype

Built directly on top of [app-ui-v3](../app-ui-v3/README.md) using the same Sky Design System token pipeline (`tokens/sky-tokens.json` -> `tools/build-tokens.mjs`) so every UX, layout, contrast, copy, and motion improvement can be reviewed and tracked before porting to `composeApp/`.

See [CHANGELOG-V4.md](CHANGELOG-V4.md) for the complete item-by-item `v3 -> v4` diff table and Kotlin/Compose target map.

## Run locally (side-by-side with v3)

- `app-ui-v3` runs on `http://127.0.0.1:5273`
- `app-ui-v4` runs on `http://127.0.0.1:5274`

```sh
cd design/app-ui-v4
npm run dev        # http://127.0.0.1:5274
npm run build      # regenerate Sky tokens + tsc --noEmit + vite build
```
