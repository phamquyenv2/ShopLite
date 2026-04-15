import type { MyUserAction, MyUserState } from '../context/MyContext';

const MyUserReducer = (current: MyUserState, action: MyUserAction): MyUserState => {
	switch (action.type) {
		case 'login':
			return action.payload;
		case 'logout':
			return null;
		default:
			return current;
	}
};

export default MyUserReducer;