#if canImport(UIKit)
import UIKit

public final class ChinaRegionPickerViewController: UIViewController {
    public typealias Completion = (RegionSelection) -> Void

    private let model: RegionPickerModel
    private let completion: Completion
    private var items: [Region] = []

    private let tableView = UITableView(frame: .zero, style: .plain)
    private let tabsScrollView = UIScrollView()
    private let tabsStackView = UIStackView()
    private lazy var tabButtons: [UIButton] = RegionLevel.allCases.map(makeTabButton)

    public init(
        store: RegionStore,
        selection: RegionSelection = .init(),
        completion: @escaping Completion
    ) {
        self.model = RegionPickerModel(store: store, selection: selection)
        self.completion = completion
        super.init(nibName: nil, bundle: nil)
        modalPresentationStyle = .pageSheet
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is unavailable") }

    public override func viewDidLoad() {
        super.viewDidLoad()
        configureUI()
        reload()
    }

    public static func present(
        from presenter: UIViewController,
        store: RegionStore,
        selection: RegionSelection = .init(),
        completion: @escaping Completion
    ) {
        let picker = ChinaRegionPickerViewController(
            store: store,
            selection: selection,
            completion: completion
        )
        presenter.present(picker, animated: true)
    }

    private func configureUI() {
        view.backgroundColor = .systemBackground
        navigationItem.title = "请选择所在地址"

        let header = UIView()
        let titleLabel = UILabel()
        titleLabel.text = "请选择所在地址"
        titleLabel.font = .systemFont(ofSize: 18, weight: .semibold)
        titleLabel.textAlignment = .center

        let closeButton = UIButton(type: .system)
        closeButton.setImage(UIImage(systemName: "xmark"), for: .normal)
        closeButton.tintColor = UIColor(white: 0.13, alpha: 1)
        closeButton.accessibilityLabel = "关闭"
        closeButton.addTarget(self, action: #selector(close), for: .touchUpInside)

        tabsScrollView.showsHorizontalScrollIndicator = false
        tabsStackView.axis = .horizontal
        tabsStackView.spacing = 18
        tabsStackView.alignment = .fill

        tableView.dataSource = self
        tableView.delegate = self
        tableView.rowHeight = 48
        tableView.tableFooterView = UIView()

        [header, tabsScrollView, tableView].forEach {
            $0.translatesAutoresizingMaskIntoConstraints = false
            view.addSubview($0)
        }
        [titleLabel, closeButton].forEach {
            $0.translatesAutoresizingMaskIntoConstraints = false
            header.addSubview($0)
        }
        tabsStackView.translatesAutoresizingMaskIntoConstraints = false
        tabsScrollView.addSubview(tabsStackView)
        tabButtons.forEach(tabsStackView.addArrangedSubview)

        NSLayoutConstraint.activate([
            header.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            header.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            header.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            header.heightAnchor.constraint(equalToConstant: 48),
            titleLabel.centerXAnchor.constraint(equalTo: header.centerXAnchor),
            titleLabel.centerYAnchor.constraint(equalTo: header.centerYAnchor),
            closeButton.trailingAnchor.constraint(equalTo: header.trailingAnchor, constant: -8),
            closeButton.centerYAnchor.constraint(equalTo: header.centerYAnchor),
            closeButton.widthAnchor.constraint(equalToConstant: 44),
            closeButton.heightAnchor.constraint(equalToConstant: 44),

            tabsScrollView.topAnchor.constraint(equalTo: header.bottomAnchor),
            tabsScrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            tabsScrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            tabsScrollView.heightAnchor.constraint(equalToConstant: 44),
            tabsStackView.leadingAnchor.constraint(equalTo: tabsScrollView.contentLayoutGuide.leadingAnchor),
            tabsStackView.trailingAnchor.constraint(equalTo: tabsScrollView.contentLayoutGuide.trailingAnchor),
            tabsStackView.topAnchor.constraint(equalTo: tabsScrollView.contentLayoutGuide.topAnchor),
            tabsStackView.bottomAnchor.constraint(equalTo: tabsScrollView.contentLayoutGuide.bottomAnchor),
            tabsStackView.heightAnchor.constraint(equalTo: tabsScrollView.frameLayoutGuide.heightAnchor),

            tableView.topAnchor.constraint(equalTo: tabsScrollView.bottomAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])

        if #available(iOS 15.0, *), let sheet = sheetPresentationController {
            sheet.detents = [.medium(), .large()]
            sheet.prefersGrabberVisible = true
        }
    }

    private func makeTabButton(level: RegionLevel) -> UIButton {
        let button = UIButton(type: .system)
        button.tag = level.rawValue
        button.titleLabel?.font = .systemFont(ofSize: 16, weight: .medium)
        button.addTarget(self, action: #selector(tabTapped(_:)), for: .touchUpInside)
        return button
    }

    private func reload() {
        do {
            items = try model.items()
            updateTabs()
            tableView.reloadData()
            scrollToSelection()
        } catch {
            items = []
            tableView.reloadData()
            presentError(error)
        }
    }

    private func updateTabs() {
        for level in RegionLevel.allCases {
            let button = tabButtons[level.rawValue]
            button.setTitle(model.title(for: level), for: .normal)
            button.isHidden = model.title(for: level).isEmpty
            button.isEnabled = model.canNavigate(to: level)
            button.tintColor = model.level == level ? .systemRed : .label
        }
    }

    private func scrollToSelection() {
        guard let selected = model.selection[model.level],
              let row = items.firstIndex(of: selected) else { return }
        tableView.scrollToRow(at: IndexPath(row: row, section: 0), at: .middle, animated: false)
    }

    private func presentError(_ error: Error) {
        let alert = UIAlertController(title: "加载失败", message: error.localizedDescription, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "确定", style: .default))
        present(alert, animated: true)
    }

    @objc private func close() { dismiss(animated: true) }

    @objc private func tabTapped(_ sender: UIButton) {
        guard let level = RegionLevel(rawValue: sender.tag), model.navigate(to: level) else { return }
        reload()
    }
}

extension ChinaRegionPickerViewController: UITableViewDataSource, UITableViewDelegate {
    public func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        items.count
    }

    public func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let identifier = "ChinaRegionPickerCell"
        let cell = tableView.dequeueReusableCell(withIdentifier: identifier)
            ?? UITableViewCell(style: .default, reuseIdentifier: identifier)
        let item = items[indexPath.row]
        let selected = model.selection[model.level]?.code == item.code
        cell.textLabel?.text = item.name
        cell.textLabel?.font = .systemFont(ofSize: 16, weight: selected ? .medium : .regular)
        cell.imageView?.image = selected ? Self.selectedIcon : nil
        cell.accessoryType = .none
        return cell
    }

    public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        if let result = model.select(items[indexPath.row]) {
            completion(result)
            dismiss(animated: true)
        } else {
            reload()
        }
    }

    private static let selectedIcon: UIImage? = {
        guard let url = Bundle.module.url(
            forResource: "icon_pcat_city_right",
            withExtension: "png",
            subdirectory: "Resources"
        ), let data = try? Data(contentsOf: url) else { return nil }
        return UIImage(data: data, scale: 2)?.withRenderingMode(.alwaysOriginal)
    }()
}
#endif
