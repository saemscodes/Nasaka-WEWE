package org.briarproject.briar.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.google.android.material.button.MaterialButton;

import org.briarproject.briar.R;
import org.briarproject.nullsafety.NotNullByDefault;

import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static androidx.transition.TransitionManager.beginDelayedTransition;

@NotNullByDefault
public class NasakaWeweButton extends FrameLayout {

	private final Button button;
	private final ProgressBar progressBar;

	public NasakaWeweButton(Context context) {
		this(context, null);
	}

	public NasakaWeweButton(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public NasakaWeweButton(Context context, @Nullable AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.nasaka_wewe_button, this, true);

		TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.NasakaWeweButton);
		String text = attributes.getString(R.styleable.NasakaWeweButton_text);
		int style = attributes
				.getResourceId(R.styleable.NasakaWeweButton_buttonStyle, 0);
		attributes.recycle();

		ContextThemeWrapper wrapper = new ContextThemeWrapper(context, style);
		button = new MaterialButton(wrapper, null, style);
		button.setText(text);
		addView(button);
		progressBar = findViewById(R.id.nasaka_wewe_button_progress_bar);
	}

	@Override
	public void setOnClickListener(@Nullable OnClickListener l) {
		if (l == null)
			button.setOnClickListener(null);
		else {
			button.setOnClickListener(v -> {
				beginDelayedTransition(this);
				progressBar.setVisibility(VISIBLE);
				button.setVisibility(INVISIBLE);
				l.onClick(this);
			});
		}
	}

	public void reset() {
		beginDelayedTransition(this);
		progressBar.setVisibility(INVISIBLE);
		button.setVisibility(VISIBLE);
	}

}
