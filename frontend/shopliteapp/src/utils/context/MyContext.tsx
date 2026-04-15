import { createContext } from 'react';
import type { Dispatch } from 'react';

export type MyUserState = unknown | null;

export type MyUserAction =
	| { type: 'login'; payload: unknown }
	| { type: 'logout' };

export type MyContextValue = [MyUserState, Dispatch<MyUserAction>];

const noopDispatch: Dispatch<MyUserAction> = () => undefined;

export const MyContext = createContext<MyContextValue>([null, noopDispatch]);