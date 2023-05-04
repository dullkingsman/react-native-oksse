import * as React from 'react';
import EventSource, { RetryError, SSEMessage } from 'react-native-oksse';

export default function App() {
  React.useEffect(() => {
    const eventSource = new EventSource(
      '<url>',
      {
        headers: { Authorization: '<token>' },
        query: { topic: '<topic>' },
      }
    );

    const locationFeedHandler = (message: SSEMessage) => {
      console.log({ LOCATION_FEED: message });
    };

    const onMessageHandler = (message: SSEMessage) => {
      console.log({ message });
    };

    const onOpenHandler = () => {
      console.log({ open: true });
    };

    const onClosedHandler = () => {
      console.log({ closed: true });
    };

    const onRetryErrorHandler = (
      err: Error | null,
      response: RetryError['response'] | null
    ) => {
      console.log({ retriedBut: { err, response } });
    };

    eventSource.addEventListener('LOCATION_FEED', locationFeedHandler);

    eventSource.onmessage = onMessageHandler;

    eventSource.onopen = onOpenHandler;

    eventSource.onclosed = onClosedHandler;

    eventSource.onretryerror = onRetryErrorHandler;

    return () => {
      eventSource.removeMany([
        locationFeedHandler,
        onMessageHandler,
        onOpenHandler,
        onClosedHandler,
        onRetryErrorHandler,
      ]);

      eventSource.close();
    };
  }, []);

  return (null);
}
