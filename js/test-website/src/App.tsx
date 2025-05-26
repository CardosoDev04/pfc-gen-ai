import { useState } from "react";
import "./App.css";

function App() {
    const [isListShown, setIsListShown] = useState(false);
    const [isSuccess, setIsSuccess] = useState(false)
    const items = ["Item 1", "Item 2", "Item 3"];
    return (
        <>
            <div className="flex flex-col w-full h-full space-y-5">
                <input
                    id="origin-input"
                    placeholder="Origem"
                    className="w-1/6 border border-black"
                    type="text"
                />
                <input
                    id="destination-input"
                    placeholder="Destino"
                    className="w-1/6 border border-black"
                    type="text"
                />
                <button
                    id="search-btn"
                    onClick={() => {
                        setIsListShown((prev) => !prev);
                    }}
                    className="w-1/6 border border-blue-500 bg-blue-500 text-white hover:cursor-pointer hover:bg-blue-600"
                >
                    Pesquisa
                </button>
                {isListShown && (
                    <div className="flex flex-col space-y-5">
                        {items.map((item, index) => (
                            <div
                                key={index}
                                className="flex justify-between p-2 result space-x-10 bg-gray-200 w-1/6 rounded-md"
                            >
                                <h1 id="item-titlu">{item}</h1>
                                <button
                                    className="book-btn p-2 bg-blue-500 hover:bg-blue-600 text-white hover:cursor-pointer"
                                    id={`book-${item.trim().toLowerCase()}-btn`}
                                    onClick={() => {setIsSuccess((prev) => !prev)}}
                                >
                                    Reservar
                                </button>
                            </div>
                        ))}
                    </div>
                )}
                {isSuccess && <div className="flex justify-center p-10 bg-green-500 w-32 h-32 text-center items-center text-white rounded-full">SUCCESS</div>}
            </div>
        </>
    );
}

export default App;
