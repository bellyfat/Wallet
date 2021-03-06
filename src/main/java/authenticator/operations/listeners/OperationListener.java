package authenticator.operations.listeners;

import javax.annotation.Nullable;

import authenticator.operations.BAOperation;

public interface OperationListener {
	// progress
	public void onBegin(BAOperation operation, String str);
	public void statusReport(BAOperation operation, String report);
	public void onFinished(BAOperation operation, String str);
	public void onError(@Nullable BAOperation operation, @Nullable Exception e, @Nullable Throwable t);
	public void onWarning(@Nullable BAOperation operation, String warning);
}
