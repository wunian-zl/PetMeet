from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont
from pygments import lex
from pygments.lexers import JavaLexer
from pygments.token import Comment, Keyword, Name, Number, String, Token


ROOT = Path(__file__).resolve().parents[1]
BACKEND = ROOT / "PetMeet-backend" / "src" / "main" / "java" / "org" / "petmeet" / "service" / "impl"
THESIS = ROOT.parent / "论文截图"


@dataclass(frozen=True)
class Job:
    source: Path
    start_line: int
    end_line: int
    output_dir: Path
    output_name: str

    @property
    def tab_name(self) -> str:
        return self.source.name

    @property
    def output(self) -> Path:
        return self.output_dir / self.output_name


JOBS: list[Job] = [
    Job(
        BACKEND / "CmsNoteServiceImpl.java",
        69,
        88,
        THESIS / "图4.1-4.2_内容发布审核与商品关联模块",
        "图4.1 代码图1_白底彩色.png",
    ),
    Job(
        BACKEND / "AdminNoteServiceImpl.java",
        165,
        216,
        THESIS / "图4.1-4.2_内容发布审核与商品关联模块",
        "图4.1 代码图2_白底彩色.png",
    ),
    Job(
        BACKEND / "OmsOrderServiceImpl.java",
        77,
        96,
        THESIS / "图4.3-4.4_商品浏览购物车与订单交易模块",
        "图4.3 代码图1_白底彩色.png",
    ),
    Job(
        BACKEND / "OmsOrderServiceImpl.java",
        181,
        204,
        THESIS / "图4.3-4.4_商品浏览购物车与订单交易模块",
        "图4.3 代码图2_白底彩色.png",
    ),
    Job(
        BACKEND / "AdminAfterSaleServiceImpl.java",
        165,
        226,
        THESIS / "图4.5-4.6_售后退款处理模块",
        "图4.5 代码图_白底彩色.png",
    ),
    Job(
        BACKEND / "CmsComplaintServiceImpl.java",
        39,
        89,
        THESIS / "图4.7-4.8_投诉处理与通知反馈模块",
        "图4.7 代码图_白底彩色.png",
    ),
]


KEYWORDS = {
    "abstract",
    "assert",
    "boolean",
    "break",
    "byte",
    "case",
    "catch",
    "char",
    "class",
    "const",
    "continue",
    "default",
    "do",
    "double",
    "else",
    "enum",
    "extends",
    "final",
    "finally",
    "float",
    "for",
    "goto",
    "if",
    "implements",
    "import",
    "instanceof",
    "int",
    "interface",
    "long",
    "native",
    "new",
    "package",
    "private",
    "protected",
    "public",
    "return",
    "short",
    "static",
    "strictfp",
    "super",
    "switch",
    "synchronized",
    "this",
    "throw",
    "throws",
    "transient",
    "try",
    "void",
    "volatile",
    "while",
    "var",
    "record",
    "sealed",
    "permits",
}


def load_font(name: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(name, size=size)


CONSOLAS = r"C:\Windows\Fonts\consola.ttf"
CONSOLAS_BOLD = r"C:\Windows\Fonts\consolab.ttf"
NOTO_SC = r"C:\Windows\Fonts\NotoSansSC-VF.ttf"
SIMSUN = r"C:\Windows\Fonts\simsun.ttc"
MSYH = r"C:\Windows\Fonts\msyh.ttc"


def is_cjk_text(text: str) -> bool:
    return any(ord(ch) > 127 for ch in text)


def measure_text(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.FreeTypeFont) -> int:
    if not text:
        return 0
    bbox = draw.textbbox((0, 0), text, font=font)
    return bbox[2] - bbox[0]


def text_height(font: ImageFont.FreeTypeFont) -> int:
    ascent, descent = font.getmetrics()
    return ascent + descent


FIELD_DECL_RE = re.compile(
    r"^\s*(?:private|protected|public)\s+"
    r"(?:static\s+final\s+)?"
    r"[\w<>\[\], ?]+?\s+"
    r"([A-Za-z_][A-Za-z0-9_]*)\s*(?:[=;,\)])"
)


def field_name_for_line(line: str) -> str | None:
    match = FIELD_DECL_RE.match(line)
    if match:
        return match.group(1)
    return None


def split_lines_with_numbers(source: Path, start_line: int, end_line: int) -> list[tuple[int, str]]:
    lines = source.read_text(encoding="utf-8").splitlines()
    selected: list[tuple[int, str]] = []
    for line_no in range(start_line, end_line + 1):
        if 1 <= line_no <= len(lines):
            selected.append((line_no, lines[line_no - 1].rstrip("\n")))
    return selected


def color_for_token(tok_type, tok_text: str, line_text: str, field_name: str | None):
    if tok_text == field_name:
        return (155, 60, 190)

    if tok_type in Comment:
        return (150, 150, 150)
    if tok_type in String:
        return (34, 153, 84)
    if tok_type in Keyword:
        return (56, 103, 214)
    if tok_type in Number:
        return (56, 103, 214)
    if tok_type in Name.Decorator:
        return (186, 142, 14)
    if tok_type in Name.Constant:
        return (160, 43, 183)
    if tok_type in Name.Class:
        return (38, 38, 38)
    if tok_type in Name.Function:
        return (38, 38, 38)
    if tok_type in Name.Builtin:
        return (38, 38, 38)
    if tok_type in Name.Variable:
        if field_name and tok_text == field_name:
            return (155, 60, 190)
        return (38, 38, 38)
    if tok_type in Name.Attribute:
        return (38, 38, 38)
    if tok_type in Name:
        return (38, 38, 38)
    if tok_type in Token.Operator or tok_type in Token.Punctuation:
        return (38, 38, 38)
    if tok_text in {"null", "true", "false"}:
        return (56, 103, 214)
    return (38, 38, 38)


def render_token_text(
    draw: ImageDraw.ImageDraw,
    x: int,
    y: int,
    text: str,
    fill: tuple[int, int, int],
    ascii_font: ImageFont.FreeTypeFont,
    cjk_font: ImageFont.FreeTypeFont,
) -> int:
    if not text:
        return x

    # Split on script boundaries so mixed Chinese/ASCII tokens still line up well.
    i = 0
    while i < len(text):
        j = i + 1
        current_cjk = is_cjk_text(text[i])
        while j < len(text) and is_cjk_text(text[j]) == current_cjk:
            j += 1
        chunk = text[i:j]
        font = cjk_font if current_cjk else ascii_font
        draw.text((x, y), chunk, font=font, fill=fill)
        x += measure_text(draw, chunk, font)
        i = j
    return x


def render_job(job: Job) -> tuple[Image.Image, Path]:
    selected = split_lines_with_numbers(job.source, job.start_line, job.end_line)
    if not selected:
        raise RuntimeError(f"No lines selected from {job.source}")

    code_font = load_font(CONSOLAS, 18)
    code_font_bold = load_font(CONSOLAS_BOLD, 18)
    num_font = load_font(CONSOLAS, 16)
    cjk_font = load_font(MSYH, 18)
    header_font = load_font(CONSOLAS_BOLD, 18)
    file_tab_font = load_font(CONSOLAS_BOLD, 17)

    scratch = Image.new("RGB", (4000, 4000), "white")
    scratch_draw = ImageDraw.Draw(scratch)

    max_line_no = max(line_no for line_no, _ in selected)
    line_no_width = measure_text(scratch_draw, str(max_line_no), num_font)
    gutter_left = 28
    gutter_right = 22
    gutter_width = max(70, line_no_width + gutter_left + gutter_right)

    code_left_pad = 24
    code_right_pad = 36
    top_pad = 18
    bottom_pad = 20
    tab_height = 48
    chrome_height = 56
    header_gap = 0

    rendered_lines: list[dict[str, object]] = []
    max_code_width = 0
    line_height = max(28, text_height(code_font) + 8)

    for line_no, line in selected:
        field_name = field_name_for_line(line)
        tokens = list(lex(line.expandtabs(4), JavaLexer()))
        token_runs: list[tuple[str, tuple[int, int, int], ImageFont.FreeTypeFont]] = []

        for tok_type, tok_text in tokens:
            if tok_text == "\n":
                continue
            fill = color_for_token(tok_type, tok_text, line, field_name)
            font = cjk_font if is_cjk_text(tok_text) else code_font
            if tok_text == field_name:
                font = code_font_bold
            token_runs.append((tok_text, fill, font))

        code_width = 0
        for tok_text, _, font in token_runs:
            code_width += measure_text(scratch_draw, tok_text, font)
        max_code_width = max(max_code_width, code_width)
        rendered_lines.append(
            {
                "line_no": line_no,
                "line": line,
                "token_runs": token_runs,
            }
        )

    body_width = gutter_width + code_left_pad + max_code_width + code_right_pad
    width = max(1380, body_width + 48)

    code_top = chrome_height + tab_height + header_gap + top_pad
    body_height = len(rendered_lines) * line_height + top_pad + bottom_pad
    height = code_top + body_height

    img = Image.new("RGB", (width, height), (255, 255, 255))
    draw = ImageDraw.Draw(img)

    # Outer frame and chrome.
    frame_rect = (20, 20, width - 20, height - 20)
    draw.rounded_rectangle(frame_rect, radius=16, fill=(255, 255, 255), outline=(232, 232, 232), width=1)
    draw.rounded_rectangle((20, 20, width - 20, 20 + chrome_height), radius=16, fill=(246, 246, 246), outline=None)
    draw.rectangle((20, 20 + chrome_height // 2, width - 20, 20 + chrome_height), fill=(246, 246, 246))

    # Window buttons and project name.
    dot_y = 20 + 18
    dot_x = 20 + 26
    for color in [(255, 95, 86), (255, 189, 46), (39, 201, 63)]:
        draw.ellipse((dot_x, dot_y, dot_x + 11, dot_y + 11), fill=color)
        dot_x += 23
    draw.text((20 + 88, 20 + 12), "PetMeet-backend", font=header_font, fill=(36, 36, 36))

    # Tab strip.
    tab_top = 20 + chrome_height
    draw.rectangle((20, tab_top, width - 20, tab_top + tab_height), fill=(248, 248, 248), outline=(233, 233, 233))
    tab_w = measure_text(draw, job.tab_name, file_tab_font) + 42
    tab_rect = (20 + 18, tab_top + 4, 20 + 18 + tab_w, tab_top + tab_height + 2)
    draw.rounded_rectangle(tab_rect, radius=12, fill=(255, 255, 255), outline=(223, 223, 223), width=1)
    draw.text((tab_rect[0] + 18, tab_top + 15), job.tab_name, font=file_tab_font, fill=(64, 64, 64))

    # Content background.
    content_top = tab_top + tab_height
    draw.rectangle((20, content_top, width - 20, height - 20), fill=(255, 255, 255))
    gutter_rect = (20, content_top, 20 + gutter_width, height - 20)
    draw.rectangle(gutter_rect, fill=(255, 255, 255))
    draw.rectangle((20 + gutter_width, content_top, 20 + gutter_width + 1, height - 20), fill=(236, 236, 236))

    # Code lines.
    start_y = code_top
    for idx, item in enumerate(rendered_lines):
        line_no = int(item["line_no"])
        token_runs = item["token_runs"]
        y = start_y + idx * line_height

        # Line numbers.
        line_no_text = str(line_no)
        num_x = 20 + gutter_width - gutter_right - measure_text(draw, line_no_text, num_font)
        draw.text((num_x, y + 1), line_no_text, font=num_font, fill=(170, 176, 186))

        x = 20 + gutter_width + code_left_pad
        for tok_text, fill, font in token_runs:
            if tok_text == "serialVersionUID":
                bbox = draw.textbbox((x - 2, y - 1), tok_text, font=font)
                draw.rounded_rectangle((bbox[0] - 4, bbox[1] + 3, bbox[2] + 4, bbox[3] + 3), radius=2, fill=(252, 246, 214))
            x = render_token_text(draw, x, y, tok_text, fill, code_font, cjk_font)

    return img, job.output


def main() -> None:
    for job in JOBS:
        img, out = render_job(job)
        out.parent.mkdir(parents=True, exist_ok=True)
        img.save(out)
        print(out)


if __name__ == "__main__":
    main()
