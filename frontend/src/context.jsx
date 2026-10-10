import { createContext, useContext } from "react";
export const ERPContext = createContext(null);
export const useERP = () => useContext(ERPContext);
