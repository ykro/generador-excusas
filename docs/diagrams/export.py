"""Renders every *.html diagram in this folder to PNG (diagram only, 2x, with paper background)."""
import asyncio
from pathlib import Path
from playwright.async_api import async_playwright

HERE = Path(__file__).parent


async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(channel="chrome")
        page = await browser.new_page(viewport={"width": 1200, "height": 900}, device_scale_factor=2)
        for html in sorted(HERE.glob("*.html")):
            await page.goto(html.resolve().as_uri())
            await page.evaluate("document.fonts.ready")
            await page.wait_for_timeout(300)
            await page.locator("svg").first.screenshot(path=str(html.with_suffix(".png")))
            print("exported", html.with_suffix(".png").name)
        await browser.close()


asyncio.run(main())
