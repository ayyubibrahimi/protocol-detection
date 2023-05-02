import uuid
import psutil
import subprocess
from kivymd.app import MDApp
from kivy.clock import Clock
from kivy.uix.boxlayout import BoxLayout
from kivymd.uix.button import MDRaisedButton
from kivymd.uix.label import MDLabel
from scapy.all import sniff, Ether, conf
import scapy 


class NetworkStatusApp(MDApp):
    def __init__(self, **kwargs):
        super(NetworkStatusApp, self).__init__(**kwargs)
        self.mac_address = self.get_mac_address()

    def get_mac_address(self):
        mac_address = ':'.join(format(c, '02x') for c in uuid.getnode().to_bytes(6, 'big'))
        return mac_address

    def build(self):
        layout = BoxLayout(orientation='vertical', padding=20, spacing=20)

        self.status_label = MDLabel(text='Press begin to check your network status', halign='center')
        layout.add_widget(self.status_label)

        self.button = MDRaisedButton(text='Begin', size_hint=(0.5, None), pos_hint={'center_x': 0.5})
        self.button.bind(on_press=self.toggle_loop)
        layout.add_widget(self.button)

        return layout

    def toggle_loop(self, *args):
        if self.button.text == 'Begin':
            self.button.text = 'Stop'
            self.status_label.text = 'Checking network status...'
            self.start_loop()
        else:
            self.button.text = 'Begin'
            self.stop_loop()

    def start_loop(self):
        self.loop = Clock.schedule_interval(self.check_network_status, 2)

    def stop_loop(self):
        self.loop.cancel()

    def check_network_status(self, dt):
        interface_name = 'Wi-Fi'
        net_io_counters = psutil.net_io_counters(pernic=True)[interface_name]
        network_type = 'Wi-Fi'

        sniff_timeout = 5
        sniff_iface = interface_name

        sniff_filter_broadcast = "arp and (ether dst ff:ff:ff:ff:ff:ff or ether dst net 33:33 or ether dst net 01:00)"
        if any(sniff(filter=sniff_filter_broadcast, timeout=sniff_timeout, iface=sniff_iface)):
            self.alert()
            self.status_label.text = 'Packets being sent to broadcast addresses or unknown addresses on your network'
        else:
            sniff_filter_arp = f"arp and not ether src {self.mac_address} and ether dst {self.mac_address}"
            if any(sniff(filter=sniff_filter_arp, timeout=sniff_timeout, iface=sniff_iface)):
                self.alert()
                self.status_label.text = 'Packets being sent with incorrect or invalid source or destination addresses'
            else:
                sniff_filter_tcp = f"tcp and not ether src {self.mac_address} and (tcp dst port 80 or tcp dst port 443)"
                if any(sniff(filter=sniff_filter_tcp, timeout=sniff_timeout, iface=sniff_iface)):
                    self.alert()
                    self.status_label.text = 'Packets being sent to well-known ports, such as port 80 (HTTP) or port 443 (HTTPS)'
                else:
                    sniff_filter_vlan = "vlan and not ether dst broadcast"
                    if any(sniff(filter=sniff_filter_vlan, timeout=sniff_timeout, iface=sniff_iface)):
                        self.alert()
                        self.status_label.text = 'Packets being sent using VLAN tagging on your network'
                    else:
                        sniff_filter_other = "not arp and not ether dst broadcast"
                        if any(sniff(filter=sniff_filter_other, timeout=sniff_timeout, iface=sniff_iface)):
                            self.alert()
                            self.status_label.text = 'Packets being sent using protocols that are not commonly used on your network'
                        else:
                            self.status_label.text = f'Connected to {network_type} network ({self.mac_address})'

    def alert(self):
        print('Are you on a public wifi connection? Your computer is possibly under attack!')

    def on_stop(self):
        self.stop_loop()

if __name__ == '__main__':
    NetworkStatusApp().run()
