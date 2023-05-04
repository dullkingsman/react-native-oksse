import {
	EmitterSubscription,
	NativeEventEmitter,
	NativeModules,
	Platform,
} from 'react-native';

const LINKING_ERROR =
	`The package 'react-native-oksse' doesn't seem to be linked. Make sure: \n\n` +
	Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
	'- You rebuilt the app after installing the package\n' +
	'- You are not using Expo Go\n';

const NativeEventSource = NativeModules.EventSource
	? NativeModules.EventSource
	: new Proxy(
			{},
			{
				get() {
					throw new Error(LINKING_ERROR);
				},
			}
	  );

export type Headers = { [key: string]: string };

export type Query = { [key: string]: string };

export type SSEConnConfig = { headers?: Headers; query?: Query };

export interface NativeEventSourceInterface {
	initialize(
		hash: string,
		args: {
			url: string;
			config?: SSEConnConfig;
		}
	): () => void;
	close(hash: string): void;
}

export type SSEMessage = {
	id: string;
	event: string;
	data: string;
};

export type RetryError = {
	throwable: { message: string; stackTrace: string };
	response: { body: string; headers: Headers; status: number };
};

const TypedNativeEventSource = NativeEventSource as NativeEventSourceInterface;

export type OnMessageHandler = (message: SSEMessage) => void;

export type OnCommentHandler = (comment: string) => void;

export type OnOpenHandler = () => void;

export type OnClosedHandler = () => void;

export type OnRetryTimeHandler = (retryTime: number) => void;

export type OnRetryErrorHandler = (
	error: Error | null,
	response: RetryError['response'] | null
) => void;

export type SSEEventHandler =
	| OnMessageHandler
	| OnCommentHandler
	| OnOpenHandler
	| OnClosedHandler
	| OnRetryTimeHandler
	| OnRetryErrorHandler;

class EventSource<T extends string> {
	hash = '';
	readyState = 0;
	url = '';

	private eventEmitter = new NativeEventEmitter(NativeEventSource);
	private subscriptions = new Map<SSEEventHandler, EmitterSubscription>();

	constructor(url: string, config?: SSEConnConfig) {
		const args = { url, config };
		const hash = JSON.stringify(args);

		this.url = url;
		this.hash = hash;

		TypedNativeEventSource.initialize(hash, args);
	}

	addEventListener(event: T, handler: OnMessageHandler) {
		const subscription = this.eventEmitter.addListener(
			'MESSAGE',
			(evt: Parameters<OnMessageHandler>[0]) => {
				if (evt?.event === event) {
					handler(evt);
				}
			}
		);
		this.subscriptions.set(handler, subscription);
	}

	set onmessage(handler: OnMessageHandler) {
		const subscription = this.eventEmitter.addListener('MESSAGE', handler);
		this.subscriptions.set(handler, subscription);
	}

	set oncomment(handler: OnCommentHandler) {
		const subscription = this.eventEmitter.addListener('COMMENT', handler);
		this.subscriptions.set(handler, subscription);
	}

	set onopen(handler: OnOpenHandler) {
		this.readyState = 1;

		const subscription = this.eventEmitter.addListener('OPEN', handler);
		this.subscriptions.set(handler, subscription);
	}

	set onclosed(handler: OnClosedHandler) {
		this.readyState = 2;

		const subscription = this.eventEmitter.addListener('CLOSED', handler);
		this.subscriptions.set(handler, subscription);
	}

	set onretrytime(handler: OnRetryTimeHandler) {
		const subscription = this.eventEmitter.addListener('RETRY_TIME', handler);
		this.subscriptions.set(handler, subscription);
	}

	set onretryerror(handler: OnRetryErrorHandler) {
		this.readyState = 3;

		const subscription = this.eventEmitter.addListener(
			'RETRY_ERROR',
			(event: RetryError) => {
				let error: Error | null = null;
				const response: RetryError['response'] | null = event.response ?? null;

				if (event.throwable) {
					error = new Error(event.throwable.message);
					error.stack = event.throwable.stackTrace;
				}

				if (response) response.status = Number(response.status);

				handler(error, response);
			}
		);
		this.subscriptions.set(handler, subscription);
	}

	remove(handler: SSEEventHandler) {
		this.subscriptions.get(handler)?.remove();
		this.subscriptions.delete(handler);
	}

	removeMany(handlers: Array<SSEEventHandler>) {
		for (const handler of handlers) this.remove(handler);
	}

	flushAllListeners() {
		for (const handler of this.subscriptions.keys()) this.remove(handler);
	}

	close() {
		TypedNativeEventSource.close(this.hash);
	}
}

export default EventSource;
