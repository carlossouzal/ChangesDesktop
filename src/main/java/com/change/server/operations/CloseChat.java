package com.change.server.operations;

import com.change.model.Chat;
import com.change.model.Item;
import com.change.model.User;
import com.change.operations.EnumOperations;
import com.change.server.ClientConnection;
import com.change.server.repository.ItemDAO;
import com.change.server.service.ChatManager;
import com.change.server.service.ClientsManager;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CloseChat extends IOperation {
    private static final EnumOperations operations = EnumOperations.CHAT_CLOSE;

    public CloseChat(IOperation next) {
        super(next);
    }

    public CloseChat() {
        super();
    }

    @Override
    public void handle(ClientConnection client, JSONObject message) throws IOException {
        List<String> mensagem = new ArrayList<>();
        if (operations.getNumber() == message.getInt("operacao")) {
            Item item = getProduto(message.getJSONObject("data").getString("produto_servico_id"));
            Chat chat = ChatManager.getInstance().getChat(item.getOwner());
            User user = ClientsManager.getInstance().get(client).getUser();

            if (null != chat && !chat.isFechando()){
                boolean flag = message.getJSONObject("data").getBoolean("flag_confirma");
                ClientConnection destiny = null;
                if(!user.equals(chat.getUsuario()))
                    destiny = ClientsManager.getInstance().get(chat.getUsuario()).getConnect();
                else
                    destiny =  ClientsManager.getInstance().get(item.getOwner()).getConnect();

                chat.addFechado(flag);
                if(flag){
                    mensagem.add("Fechando negocio");
                }else{
                    ChatManager.getInstance().removeChat(item.getOwner());
                    mensagem.add("Negocio Cancelado");
                }
                destiny.send(makeNotification(flag, item, mensagem).toString());
            }
        } else {
            super.handle(client, message);
        }
    }

    private JSONObject makeNotification(boolean flag, Item prod, List<String> mensagem){
        JSONObject obj = new JSONObject();
        obj.put("operacao", EnumOperations.FECHA_NEGOCIO_CLIENT.getNumber());
        obj.put("erro", flag);
        obj.put("mensagem", mensagem);
        obj.put("data", new JSONObject()
                .put("produto_servico_id", prod.getCode())
        );
        return obj;
    }

    private Item getProduto(String code) {
        return ItemDAO.getInstance().get(code);
    }
}
