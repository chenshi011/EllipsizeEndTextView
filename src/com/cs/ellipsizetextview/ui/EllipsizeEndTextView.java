package com.cs.ellipsizetextview.ui;

import com.cs.ellipsizetextview.R;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.widget.TextView;

public class EllipsizeEndTextView extends TextView {
    private float mLastLineMaxWidthScale = 1.0f; 
    private boolean isEllipsized;
    private boolean isStale;
    private boolean programmaticChange;
    private String fullText;
    private float lineSpacingMultiplier = 1.0f;
    private float lineAdditionalVerticalPadding = 0.0f;

    public EllipsizeEndTextView(Context context) {
        super(context);
    }

    public EllipsizeEndTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        readAttrs(context, attrs);
    }

    public EllipsizeEndTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        readAttrs(context, attrs);
    }

    private void readAttrs(Context context, AttributeSet attrs) {
        TypedArray attributes = context.obtainStyledAttributes(attrs,
                R.styleable.EllipsizeEndTextView, R.style.EllipsizeTextView_Default, 0);
        mLastLineMaxWidthScale = attributes.getFloat(R.styleable.EllipsizeEndTextView_lastlscale,
                1.0f);
        attributes.recycle();
    }
    
    public boolean isEllipsized() {
        return isEllipsized;
    }

    @Override
    public void setLineSpacing(float add, float mult) {
        this.lineAdditionalVerticalPadding = add;
        this.lineSpacingMultiplier = mult;
        super.setLineSpacing(add, mult);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        super.onTextChanged(text, start, before, after);
        if (!programmaticChange) {
            fullText = text.toString();
            isStale = true;
        }
    }
    
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private int getMaxLineCount() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
           return getMaxLines();
       return 0; // add custom attr to get
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isStale && getMeasuredWidth() > 0) {
            super.setEllipsize(null);
            resetText();
        }
        super.onDraw(canvas);
    }

    private void resetText() {
        int maxLines = getMaxLineCount(); 
        if (maxLines == -1) {
            return;
        }
        CharSequence text = fullText;
        if (text == null || text.length() == 0) {
            return;
        }
        Layout layout = getLayout();
        if (layout == null) {
            layout = createWorkingLayout(text);
        }
        // find the last line of text and chop it according to available space
        int linCount = layout.getLineCount();
        if (maxLines > linCount){
            return;
        }
        final int lastLineStart = layout.getLineStart(maxLines - 1);
        final CharSequence remainder = TextUtils.ellipsize(text.subSequence(lastLineStart,
                text.length()), getPaint(), layout.getWidth() * mLastLineMaxWidthScale, TextUtils.TruncateAt.END);
        // assemble just the text portion, without spans
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(text.toString(), 0, lastLineStart);
        if (!TextUtils.isEmpty(remainder)) {
            builder.append(remainder.toString());
        }
        // Now copy the original spans into the assembled string, modified for any ellipsizing.
        //
        // Merely assembling the Spanned pieces together would result in duplicate CharacterStyle
        // spans in the assembled version if a CharacterStyle spanned across the lastLineStart
        // offset.
        if (text instanceof Spanned) {
            final Spanned s = (Spanned) text;
            final Object[] spans = s.getSpans(0, s.length(), Object.class);
            final int destLen = builder.length();
            for (int i = 0; i < spans.length; i++) {
                final Object span = spans[i];
                final int start = s.getSpanStart(span);
                final int end = s.getSpanEnd(span);
                final int flags = s.getSpanFlags(span);
                if (start <= destLen) {
                    builder.setSpan(span, start, Math.min(end, destLen), flags);
                }
            }
        }
        if (!builder.equals(getText())) {
            programmaticChange = true;
            try {
                setText(builder);
            } finally {
                programmaticChange = false;
            }
        }
        isStale = false;
        boolean ellipsized = !builder.toString().equals(text);
        if (ellipsized != isEllipsized) {
            isEllipsized = ellipsized;
        }
    }

    private Layout createWorkingLayout(CharSequence workingText) {
        return new StaticLayout(workingText, getPaint(), getWidth() - getPaddingLeft() - getPaddingRight(),
                Alignment.ALIGN_NORMAL, lineSpacingMultiplier, lineAdditionalVerticalPadding, false);
    }

    @Override
    public void setEllipsize(TruncateAt where) {
        // Ellipsize settings are not respected
    }
}